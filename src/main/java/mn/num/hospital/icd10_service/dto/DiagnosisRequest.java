package mn.num.hospital.icd10_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisRequest {
    private String diagnosis;
    private int resultLimit = 10;
}