package mn.num.hospital.icd10_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mn.num.hospital.icd10_service.domain.Category;
import mn.num.hospital.icd10_service.domain.Chapter;
import mn.num.hospital.icd10_service.domain.ICD10Code;
import mn.num.hospital.icd10_service.domain.Subcategory;
import mn.num.hospital.icd10_service.dto.ICD10CodeDTO;
import mn.num.hospital.icd10_service.dto.ICD10CodeMapper;
import mn.num.hospital.icd10_service.repo.CategoryRepository;
import mn.num.hospital.icd10_service.repo.ChapterRepository;
import mn.num.hospital.icd10_service.repo.ICD10CodeRepository;
import mn.num.hospital.icd10_service.repo.SubcategoryRepository;

@Service
@Slf4j
public class Icd10DataSyncService {

    private final ExternalICD10Service externalIcd10Service;
    private final ICD10CodeRepository icd10CodeRepository;
    private final ChapterRepository chapterRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final boolean syncOnStartup;

    public Icd10DataSyncService(
            ExternalICD10Service externalIcd10Service,
            ICD10CodeRepository icd10CodeRepository,
            ChapterRepository chapterRepository,
            CategoryRepository categoryRepository,
            SubcategoryRepository subcategoryRepository,
            @Value("${icd10.sync.on-startup:true}") boolean syncOnStartup) {
        this.externalIcd10Service = externalIcd10Service;
        this.icd10CodeRepository = icd10CodeRepository;
        this.chapterRepository = chapterRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.syncOnStartup = syncOnStartup;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartupIfNeeded() {
        if (!syncOnStartup) {
            log.info("ICD-10 startup sync disabled");
            return;
        }

        if (icd10CodeRepository.count() > 0) {
            log.info("ICD-10 dataset already loaded, skipping startup sync");
            return;
        }

        syncFromExternalSource();
    }

    public String syncFromExternalSource() {
        log.info("ICD-10 data sync started");
        List<ICD10CodeDTO> allCodes = externalIcd10Service.fetchAllCodes();

        if (allCodes.isEmpty()) {
            log.warn("ICD-10 data sync skipped because external source returned no data");
            return "No ICD-10 data received from external source";
        }

        int saved = 0;
        int skipped = 0;

        for (ICD10CodeDTO dto : allCodes) {
            if (icd10CodeRepository.findFirstByCodeIgnoreCase(dto.getCode()).isPresent()) {
                skipped++;
                continue;
            }

            icd10CodeRepository.save(convertToEntity(dto));
            saved++;
        }

        String message = "ICD-10 data sync finished: saved=" + saved + ", skipped=" + skipped;
        log.info(message);
        return message;
    }

    public boolean isDataLoaded() {
        return icd10CodeRepository.count() > 0;
    }

    private ICD10Code convertToEntity(ICD10CodeDTO dto) {
        ICD10Code entity = ICD10CodeMapper.toEntity(dto);

        if (dto.getChapterCode() != null) {
            var chapter = chapterRepository.findByChapter(dto.getChapterCode())
                    .orElseGet(() -> {
                        Chapter newChapter = new Chapter();
                        newChapter.setChapter(dto.getChapterCode());
                        newChapter.setName(dto.getChapterName());
                        return chapterRepository.save(newChapter);
                    });
            entity.setChapter(chapter);
        }

        if (dto.getCategoryRange() != null) {
            var category = categoryRepository.findByRange(dto.getCategoryRange())
                    .orElseGet(() -> {
                        Category newCategory = new Category();
                        newCategory.setRange(dto.getCategoryRange());
                        newCategory.setName(dto.getCategoryName());
                        if (dto.getChapterCode() != null) {
                            chapterRepository.findByChapter(dto.getChapterCode()).ifPresent(newCategory::setChapter);
                        }
                        return categoryRepository.save(newCategory);
                    });
            entity.setCategory(category);
        }

        if (dto.getSubcategoryCode() != null) {
            var subcategory = subcategoryRepository.findByCode(dto.getSubcategoryCode())
                    .orElseGet(() -> {
                        Subcategory newSubcategory = new Subcategory();
                        newSubcategory.setCode(dto.getSubcategoryCode());
                        newSubcategory.setName(dto.getSubcategoryName());
                        if (dto.getCategoryRange() != null) {
                            categoryRepository.findByRange(dto.getCategoryRange()).ifPresent(newSubcategory::setCategory);
                        }
                        return subcategoryRepository.save(newSubcategory);
                    });
            entity.setSubcategory(subcategory);
        }

        return entity;
    }
}
