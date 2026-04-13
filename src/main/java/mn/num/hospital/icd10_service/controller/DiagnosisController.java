package mn.num.hospital.icd10_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mn.num.hospital.icd10_service.dto.ApiError;
import mn.num.hospital.icd10_service.dto.ApiResponse;
import mn.num.hospital.icd10_service.dto.DiagnosisSearchMeta;
import mn.num.hospital.icd10_service.dto.DiagnosisSearchResult;
import mn.num.hospital.icd10_service.service.Icd10SearchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/diagnosis")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class DiagnosisController {

    private final Icd10SearchService icd10SearchService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<DiagnosisSearchResult>>> search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "limit", required = false) Integer limit) {

        String normalizedQuery = icd10SearchService.normalizeTextQuery(query);
        int safeLimit = icd10SearchService.sanitizeLimit(limit);

        if (!icd10SearchService.isSearchable(normalizedQuery)) {
            DiagnosisSearchMeta meta = new DiagnosisSearchMeta(normalizedQuery, safeLimit, 0);
            return ResponseEntity.ok(ApiResponse.success(List.of(), meta));
        }

        log.info("Diagnosis search request: query='{}', limit={}", normalizedQuery, safeLimit);
        List<DiagnosisSearchResult> results = icd10SearchService.search(normalizedQuery, safeLimit);
        DiagnosisSearchMeta meta = new DiagnosisSearchMeta(normalizedQuery, safeLimit, results.size());
        return ResponseEntity.ok(ApiResponse.success(results, meta));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<DiagnosisSearchResult>> findByCode(@PathVariable String code) {
        return icd10SearchService.findByCode(code)
                .map(result -> ResponseEntity.ok(ApiResponse.success(result)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(
                                "Diagnosis code not found",
                                List.of(new ApiError("code", "No ICD-10 diagnosis found for the provided code")))));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("ICD-10 lookup service is running"));
    }
}
