package mn.num.hospital.icd10_service.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import mn.num.hospital.icd10_service.domain.Category;
import mn.num.hospital.icd10_service.domain.ICD10Code;
import mn.num.hospital.icd10_service.domain.Subcategory;
import mn.num.hospital.icd10_service.dto.ChapterDto;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface ICD10CodeRepository extends JpaRepository<ICD10Code, Long> {

	// 1. Code-оор хайх
    Optional<ICD10Code> findByCode(String code);

    // 2. Keyword search (name + detail)
    @Query("SELECT i FROM ICD10Code i WHERE " +
           "LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(i.detail) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY i.relevanceScore DESC")
    List<ICD10Code> searchByKeyword(@Param("keyword") String keyword);
    
    // 3. Keyword + LIMIT (Pageable ашиглана)
    @Query("SELECT i FROM ICD10Code i WHERE " +
           "LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY COALESCE(i.relevanceScore, 0) DESC")
    List<ICD10Code> findByKeywordWithLimit(@Param("keyword") String keyword,
                                           Pageable pageable);  
    // Бүх chapter-уудыг авах
    @Query("""
    	    SELECT DISTINCT new mn.num.hospital.icd10_service.dto.ChapterDto(
    	        ch.chapter, ch.name, ch.orderIndex
    	    )
    	    FROM ICD10Code i
    	    JOIN i.subcategory s
    	    JOIN s.category c
    	    JOIN c.chapter ch
    	    ORDER BY ch.orderIndex
    	""")
    List<ChapterDto> findDistinctChapters();

    // Chapter-аар category жагсаах
    @Query("SELECT DISTINCT CONCAT (c.rangeStart, '|', c.range, ' ', c.name) "
    		+ "FROM ICD10Code i "
    		+ "JOIN i.subcategory s "
    		+ "JOIN s.category c "
    		+ "JOIN c.chapter ch "
    		+ "WHERE ch.chapter = :chapter "
    		+ "ORDER BY CONCAT (c.rangeStart, '|', c.range, ' ', c.name)")
    List<String> findCategoriesByChapter(@Param("chapter") String chapter);

    // Category-аар subcategory жагсаах
    @Query("SELECT DISTINCT CONCAT(s.code, ' ', s.name) "
    		+ "FROM ICD10Code i "
    		+ "JOIN i.subcategory s "
    		+ "JOIN s.category c "
    		+ "WHERE c.range = :category "
    		+ "ORDER BY CONCAT(s.code, ' ', s.name)")
    List<String> findSubcategoriesByCategory(@Param("category") String category);
}