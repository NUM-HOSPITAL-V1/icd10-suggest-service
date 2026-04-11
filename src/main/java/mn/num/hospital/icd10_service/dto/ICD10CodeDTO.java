package mn.num.hospital.icd10_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ICD10CodeDTO {
    private String code;
    private String name;
    private String detail;
    
    // Chapter
    private String chapterCode; // "I", "II"
    private String chapterName;
    
    // Category
    private String categoryRange;
    private String categoryName;
    
    // Subcategory
    private String subcategoryCode; // "A00"
    private String subcategoryName;
    
    private Double relevanceScore = 0.0;
    
    public void setRelevanceScore(Double relevanceScore) {
    	this.relevanceScore = relevanceScore != null ? relevanceScore: 0.0;
    }
}