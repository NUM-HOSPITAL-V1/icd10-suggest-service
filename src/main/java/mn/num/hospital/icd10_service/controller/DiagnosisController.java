package mn.num.hospital.icd10_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mn.num.hospital.icd10_service.dto.DiagnosisRequest;
import mn.num.hospital.icd10_service.dto.DiagnosisResponse;
import mn.num.hospital.icd10_service.service.DiagnosisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/diagnosis")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class DiagnosisController {

    private static final int DEFAULT_LIMIT = 10;

    private final DiagnosisService diagnosisService;

    /**
     * ICD-10 код санал болгох
     * POST /diagnosis/suggest
     */
    @PostMapping("/suggest")
    public ResponseEntity<DiagnosisResponse> suggestCodes(
            @RequestBody DiagnosisRequest request) {

        String diagnosis = request.getDiagnosis();
        int limit = resolveLimit(request.getResultLimit());

        log.info("Diagnosis suggest request: diagnosis='{}', limit={}",
                diagnosis, limit);

        DiagnosisResponse response =
                diagnosisService.suggestCodes(diagnosis, limit);

        return buildResponse(response);
    }

    /**
     * Service health check
     * GET /diagnosis/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ICD-10 Service is running ✓");
    }

    /**
     * Sample data initialize
     * POST /diagnosis/init
     */
    @PostMapping("/init")
    public ResponseEntity<String> initData() {

        try {
            log.info("Sample data initialized");

            return ResponseEntity.ok("Sample data created successfully");

        } catch (Exception e) {

            log.error("Sample data initialization failed", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Initialization error: " + e.getMessage());
        }
    }

    /**
     * Limit validation
     */
    private int resolveLimit(int requestedLimit) {
        return requestedLimit > 0 ? requestedLimit : DEFAULT_LIMIT;
    }

    /**
     * API response builder
     */
    private ResponseEntity<DiagnosisResponse> buildResponse(
            DiagnosisResponse response) {

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}