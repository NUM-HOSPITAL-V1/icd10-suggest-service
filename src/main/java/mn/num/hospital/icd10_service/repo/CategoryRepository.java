package mn.num.hospital.icd10_service.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mn.num.hospital.icd10_service.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Long>{
	Optional<Category> findByRange(String range); // "A00-A09"
	Optional<Category> findByRangeStart(String rangeStart); // "A00"
}
