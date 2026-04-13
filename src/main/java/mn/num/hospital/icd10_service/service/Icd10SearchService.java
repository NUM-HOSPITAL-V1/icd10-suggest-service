package mn.num.hospital.icd10_service.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import mn.num.hospital.icd10_service.domain.ICD10Code;
import mn.num.hospital.icd10_service.dto.DiagnosisSearchResult;
import mn.num.hospital.icd10_service.repo.ICD10CodeRepository;

@Service
public class Icd10SearchService {

    public static final String SOURCE = "icd10";
    public static final int DEFAULT_LIMIT = 10;
    public static final int MAX_LIMIT = 20;
    public static final int MIN_QUERY_LENGTH = 2;
    private static final int SEARCH_CANDIDATE_LIMIT = 100;

    private final ICD10CodeRepository icd10CodeRepository;

    public Icd10SearchService(ICD10CodeRepository icd10CodeRepository) {
        this.icd10CodeRepository = icd10CodeRepository;
    }

    public List<DiagnosisSearchResult> search(String rawQuery, Integer requestedLimit) {
        String normalizedQuery = normalizeTextQuery(rawQuery);
        if (!isSearchable(normalizedQuery)) {
            return List.of();
        }

        int limit = sanitizeLimit(requestedLimit);
        String searchToken = normalizedQuery.toLowerCase(Locale.ROOT);
        var pageable = PageRequest.of(0, SEARCH_CANDIDATE_LIMIT);

        return icd10CodeRepository.findSearchCandidates(searchToken, pageable).stream()
                .map(code -> score(code, normalizedQuery))
                .flatMap(Optional::stream)
                .sorted(Comparator
                        .comparingDouble(ScoredDiagnosis::score).reversed()
                        .thenComparing(item -> item.result().code(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(item -> item.result().name(), String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .map(ScoredDiagnosis::result)
                .toList();
    }

    public Optional<DiagnosisSearchResult> findByCode(String rawCode) {
        String normalizedCode = normalizeCode(rawCode);
        if (normalizedCode.isBlank()) {
            return Optional.empty();
        }

        return icd10CodeRepository.findFirstByCodeIgnoreCase(normalizedCode)
                .map(code -> toResult(code, null));
    }

    public int sanitizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    public String normalizeTextQuery(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    public String normalizeCode(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replace(" ", "").toUpperCase(Locale.ROOT);
    }

    public boolean isSearchable(String normalizedQuery) {
        return normalizedQuery != null && normalizedQuery.length() >= MIN_QUERY_LENGTH;
    }

    private Optional<ScoredDiagnosis> score(ICD10Code code, String normalizedQuery) {
        String normalizedCode = normalizeCode(code.getCode());
        String normalizedName = normalizeTextQuery(code.getName()).toLowerCase(Locale.ROOT);
        String normalizedDetail = normalizeTextQuery(code.getDetail()).toLowerCase(Locale.ROOT);
        String queryText = normalizedQuery.toLowerCase(Locale.ROOT);
        String queryCode = normalizeCode(normalizedQuery);

        double score = 0.0;

        if (!queryCode.isBlank() && normalizedCode.equals(queryCode)) {
            score = 1.0;
        } else if (!queryCode.isBlank() && normalizedCode.startsWith(queryCode)) {
            score = 0.9;
        } else if (normalizedName.equals(queryText)) {
            score = 0.8;
        } else if (normalizedName.contains(queryText)) {
            score = 0.7;
        } else if (!normalizedDetail.isBlank() && normalizedDetail.contains(queryText)) {
            score = 0.5;
        }

        long wordMatches = Stream.of(queryText.split(" "))
                .filter(token -> !token.isBlank())
                .filter(token -> normalizedName.contains(token) || normalizedDetail.contains(token))
                .count();
        score += Math.min(wordMatches * 0.05, 0.15);

        if (score <= 0.0) {
            return Optional.empty();
        }

        return Optional.of(new ScoredDiagnosis(toResult(code, score), score));
    }

    private DiagnosisSearchResult toResult(ICD10Code code, Double score) {
        String detail = code.getDetail();
        String categoryCode = code.getCategory() != null ? code.getCategory().getRangeStart() : null;
        String categoryName = code.getCategory() != null ? code.getCategory().getName() : null;
        String chapterCode = code.getChapter() != null ? code.getChapter().getChapter() : null;
        String chapterName = code.getChapter() != null ? code.getChapter().getName() : null;

        return new DiagnosisSearchResult(
                SOURCE + ":" + code.getCode(),
                code.getCode(),
                code.getName(),
                detail == null || detail.isBlank() ? null : detail,
                chapterCode,
                chapterName,
                categoryCode,
                categoryName,
                score,
                SOURCE);
    }

    private record ScoredDiagnosis(DiagnosisSearchResult result, double score) {
    }
}
