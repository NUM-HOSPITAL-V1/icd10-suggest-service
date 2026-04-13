package mn.num.hospital.icd10_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiagnosisSearchResult(
        String id,
        String code,
        String name,
        String detail,
        String chapterCode,
        String chapterName,
        String categoryCode,
        String categoryName,
        Double score,
        String source) {
}
