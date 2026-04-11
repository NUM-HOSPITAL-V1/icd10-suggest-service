package mn.num.hospital.icd10_service.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import mn.num.hospital.icd10_service.domain.Category;
import mn.num.hospital.icd10_service.domain.Chapter;
import mn.num.hospital.icd10_service.domain.ICD10Code;
import mn.num.hospital.icd10_service.domain.Subcategory;
import mn.num.hospital.icd10_service.dto.DiagnosisResponse;
import mn.num.hospital.icd10_service.dto.ICD10CodeDTO;
import mn.num.hospital.icd10_service.dto.ICD10CodeMapper;
import mn.num.hospital.icd10_service.repo.CategoryRepository;
import mn.num.hospital.icd10_service.repo.ChapterRepository;
import mn.num.hospital.icd10_service.repo.ICD10CodeRepository;
import mn.num.hospital.icd10_service.repo.SubcategoryRepository;

@Service
@Slf4j
public class DiagnosisService {

    private final ExternalICD10Service externalService;
    private final ICD10CodeRepository icd10Repo;
    private final ChapterRepository chapterRepo;
    private final CategoryRepository categoryRepo;
    private final SubcategoryRepository subcategoryRepo;

    public DiagnosisService(ExternalICD10Service externalService, 
                            SubcategoryRepository subcategoryRepo, 
                            ICD10CodeRepository icd10Repo, 
                            ChapterRepository chapterRepo, 
                            CategoryRepository categoryRepo) {
        this.externalService = externalService;
        this.icd10Repo = icd10Repo;
		this.chapterRepo = chapterRepo;
		this.categoryRepo = categoryRepo;
		this.subcategoryRepo = subcategoryRepo;
    }
	
	@PostConstruct
	public void init() {
		if(icd10Repo.count() == 0) {
			seedFromExternalAPI();
		}
	}

	public List<String> getChapters() {
	    return chapterRepo.findAll()
	            .stream()
	            .sorted(Comparator.comparing(Chapter::getOrderIndex))
	            .map(ch -> ch.getChapter() + " " + ch.getName())
	            .toList();
	}
	
    public List<String> getCategoriesByChapter(String chapter) {
        if (chapter == null || chapter.isBlank()) return List.of();
        return icd10Repo.findCategoriesByChapter(chapter);
    }

    public List<String> getSubcategoriesByCategory(String category) {
        if (category == null || category.isBlank()) return List.of();
        return icd10Repo.findSubcategoriesByCategory(category);
    }
    
    // API-с бүх кодыг DB-д нэг удаа татаж хадгалах
    public String seedFromExternalAPI() {
        log.info("DB seed эхэллээ...");
        List<ICD10CodeDTO> allCodes = externalService.fetchAllCodes();

        if (allCodes.isEmpty()) {
            return "API-с өгөгдөл ирсэнгүй";
        }

        int saved = 0, skipped = 0;
        for (ICD10CodeDTO dto : allCodes) {
            if (icd10Repo.findByCode(dto.getCode()).isEmpty()) {
            	icd10Repo.save(convertToEntity(dto));
                saved++;
            } else {
                skipped++;
            }
        }

        String msg = String.format("Seed дууслаа: %d хадгалсан, %d давхардсан орхисон", saved, skipped);
        log.info(msg);
        return msg;
    }

    public DiagnosisResponse suggestCodes(String diagnosis, int limit) {

        long startTime = System.currentTimeMillis();
        DiagnosisResponse response = new DiagnosisResponse();
        response.setDiagnosis(diagnosis);

        if (isInvalidDiagnosis(diagnosis)) {
            return buildErrorResponse(response, "Онош буруу байна", startTime);
        }

        try {
            log.info("Хайлт эхэллээ: '{}'", diagnosis);

            CompletableFuture<List<ICD10CodeDTO>> localFuture =
                    CompletableFuture.supplyAsync(() -> getLocalResults(diagnosis, limit));

            CompletableFuture<List<ICD10CodeDTO>> externalFuture =
                    CompletableFuture.supplyAsync(() ->
                            externalService.fetchFromExternalAPI(diagnosis, limit));

            CompletableFuture<List<ICD10CodeDTO>> combinedFuture =
                    localFuture.thenCombine(externalFuture, (local, external) -> {
          
                        CompletableFuture.runAsync(() -> {
                            try {
                                saveExternalResults(external);
                            } catch (Exception e) {
                                log.error("Гадаад үр дүн хадгалахад алдаа: {}", e.getMessage());
                            }
                        });
                        return Stream.concat(local.stream(), external.stream())
                                .collect(Collectors.toList());
                    });

            List<ICD10CodeDTO> suggestions = combinedFuture
                    .get(10, TimeUnit.SECONDS);

            suggestions.forEach(s ->
                    s.setRelevanceScore(calculateScore(s, diagnosis)));

            suggestions = removeDuplicates(suggestions).stream()
                    .filter(s -> s.getRelevanceScore() > 0.2)
                    .sorted(Comparator.comparingDouble(ICD10CodeDTO::getRelevanceScore).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());

            response.setSuggestions(suggestions);
            response.setSuccess(true);
            response.setMessage(suggestions.size() + " код санал болгосон");

        } catch (TimeoutException e) {
            log.error("Хайлт timeout боллоо", e);
            return buildErrorResponse(response, "Хайлт хэтэрхий удаан байна", startTime);
        } catch (Exception e) {
            log.error("Алдаа:", e);
            return buildErrorResponse(response, "Алдаа гарлаа", startTime);
        }

        response.setResponseTime(System.currentTimeMillis() - startTime);
        return response;
    }

    private List<ICD10CodeDTO> getLocalResults(String diagnosis, int limit) {

        log.debug("Local DB query...");

        return icd10Repo.findByKeywordWithLimit(
                diagnosis,
                org.springframework.data.domain.PageRequest.of(0, limit)
        )
        .stream()
        .map(ICD10CodeMapper::toDto)
        .collect(Collectors.toList());
    }
    
    private void saveExternalResults(List<ICD10CodeDTO> externalResults) {
        for (ICD10CodeDTO dto : externalResults) {
            if (icd10Repo.findByCode(dto.getCode()).isEmpty()) {
                ICD10Code entity = convertToEntity(dto);
                icd10Repo.save(entity);
                log.debug("Saved: {} - {}", entity.getCode(), entity.getName());
            }
        }
    }
    
    private List<ICD10CodeDTO> removeDuplicates(List<ICD10CodeDTO> list) {
        return new ArrayList<>(
                list.stream()
                        .collect(Collectors.toMap(
                                ICD10CodeDTO::getCode,
                                x -> x,
                                (a, b) -> a
                        ))
                        .values()
        );
    }

    private double calculateScore(ICD10CodeDTO dto, String diagnosis) {
        String keyword = diagnosis.toLowerCase();
        String name = dto.getName().toLowerCase();
        double score = 0;
        if (name.equals(keyword))    score += 1.0;
        if (name.contains(keyword))  score += 0.6;
        for (String word : Arrays.stream(keyword.split(" ")).distinct().toList()) {
            if (name.contains(word)) score += 0.2;
        }
        return Math.min(score, 1.0);
    }

    private boolean isInvalidDiagnosis(String diagnosis) {
        return diagnosis == null
                || diagnosis.trim().length() < 2
                || diagnosis.trim().length() > 100;
    }

    private DiagnosisResponse buildErrorResponse(DiagnosisResponse response,
                                                  String message, long startTime) {
        response.setSuccess(false);
        response.setMessage(message);
        response.setSuggestions(Collections.emptyList());
        response.setResponseTime(System.currentTimeMillis() - startTime);
        return response;
    }

    private ICD10CodeDTO convertToDTO(ICD10Code code) {
        return ICD10CodeMapper.toDto(code);
    }

    private ICD10Code convertToEntity(ICD10CodeDTO dto) {
        ICD10Code entity = ICD10CodeMapper.toEntity(dto);

        // Chapter
        if (dto.getChapterCode() != null) {
            var chapter = chapterRepo.findByChapter(dto.getChapterCode())
                    .orElseGet(() -> {
                        Chapter ch = new Chapter();
                        ch.setChapter(dto.getChapterCode());
                        ch.setName(dto.getChapterName());
                        return chapterRepo.save(ch);
                    });
            entity.setChapter(chapter);
        }

        // Category
        if (dto.getCategoryRange() != null) {
            var category = categoryRepo.findByRange(dto.getCategoryRange())
                    .orElseGet(() -> {
                        Category c = new Category();
                        c.setRange(dto.getCategoryRange());
                        c.setName(dto.getCategoryName());
                        
                        if (dto.getChapterCode() != null) {
                            chapterRepo.findByChapter(dto.getChapterCode())
                                    .ifPresent(c::setChapter);
                        }
                        return categoryRepo.save(c);
                    });
            entity.setCategory(category);
        }

        // Subcategory
        if (dto.getSubcategoryCode() != null) {
            var sub = subcategoryRepo.findByCode(dto.getSubcategoryCode())
                    .orElseGet(() -> {
                        Subcategory s = new Subcategory();
                        s.setCode(dto.getSubcategoryCode());
                        s.setName(dto.getSubcategoryName());
                        
                        if (dto.getCategoryRange() != null) {
                            categoryRepo.findByRange(dto.getCategoryRange())
                                    .ifPresent(s::setCategory);
                        }
                        
                        return subcategoryRepo.save(s);
                    });
            entity.setSubcategory(sub);
        }

        return entity;
    }

    
}