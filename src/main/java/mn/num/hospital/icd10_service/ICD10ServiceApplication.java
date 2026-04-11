package mn.num.hospital.icd10_service;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;
import mn.num.hospital.icd10_service.repo.ChapterRepository;
import mn.num.hospital.icd10_service.repo.ICD10CodeRepository;
import mn.num.hospital.icd10_service.service.DiagnosisService;
import mn.num.hospital.icd10_service.ui.DiagnosisSearchUI;

@SpringBootApplication
@Slf4j
public class ICD10ServiceApplication {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");

        var context = SpringApplication.run(ICD10ServiceApplication.class, args);

        var service     = context.getBean(DiagnosisService.class);
        var chapterRepo = context.getBean(ChapterRepository.class);

        log.info("ICD-10 Diagnosis Service эхэллээ...");
        log.info("API документ: http://localhost:8080/api/swagger-ui.html");
        log.info("Health check: http://localhost:8080/api/diagnosis/health");

        javax.swing.SwingUtilities.invokeLater(() ->
            new DiagnosisSearchUI(service, chapterRepo)
        );
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
    
    
}