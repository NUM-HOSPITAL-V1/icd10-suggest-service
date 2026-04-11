package mn.num.hospital.icd10_service.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mn.num.hospital.icd10_service.domain.Subcategory;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Long>{
	Optional<Subcategory> findByCode(String code); // "A00"
}
