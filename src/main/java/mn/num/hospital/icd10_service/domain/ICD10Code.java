package mn.num.hospital.icd10_service.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "icd10_codes", indexes = { 
		@Index(name = "idx_code", columnList = "code"),
		@Index(name = "idx_name", columnList = "name") 
	})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ICD10Code {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; 
	
	@Column(unique=true, nullable = false)
	private String code; // A00.0
	
	@Column(nullable = false, columnDefinition = "TEXT")
	private String name;
	
	@Column(columnDefinition = "TEXT")
	private String detail;
	
	@ManyToOne(optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

	@ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
	@ManyToOne(optional=false)
	@JoinColumn(name="subcategory_id", nullable=false)
	private Subcategory subcategory;	
	
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "relevance_score")
    private Double relevanceScore;
    
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}