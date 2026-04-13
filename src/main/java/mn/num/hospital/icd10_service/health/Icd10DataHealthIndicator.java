package mn.num.hospital.icd10_service.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import mn.num.hospital.icd10_service.repo.ICD10CodeRepository;

@Component("icdData")
public class Icd10DataHealthIndicator implements HealthIndicator {

    private final ICD10CodeRepository icd10CodeRepository;

    public Icd10DataHealthIndicator(ICD10CodeRepository icd10CodeRepository) {
        this.icd10CodeRepository = icd10CodeRepository;
    }

    @Override
    public Health health() {
        long count = icd10CodeRepository.count();
        if (count > 0) {
            return Health.up()
                    .withDetail("datasetLoaded", true)
                    .withDetail("recordCount", count)
                    .withDetail("searchAvailable", true)
                    .build();
        }

        return Health.down()
                .withDetail("datasetLoaded", false)
                .withDetail("recordCount", count)
                .withDetail("searchAvailable", false)
                .build();
    }
}
