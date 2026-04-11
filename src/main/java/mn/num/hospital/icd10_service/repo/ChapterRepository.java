package mn.num.hospital.icd10_service.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mn.num.hospital.icd10_service.domain.Chapter;

public interface ChapterRepository extends JpaRepository<Chapter, Long>{

	Optional<Chapter> findByChapter(String chapter); // "I", "II" 
}
