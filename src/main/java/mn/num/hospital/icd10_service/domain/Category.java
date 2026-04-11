package mn.num.hospital.icd10_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table(name="categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id; 
	
	@Column(nullable = false)
	private String range; // "A00-A09"
	
	@Column(nullable = false)
	private String rangeStart; // "A00"
	
	@Column(columnDefinition = "TEXT")
	private String name;
	
	@ManyToOne
	@JoinColumn(name="chapter_id")
	private Chapter chapter;
	
	@PrePersist
	@PreUpdate
	public void deriveRangeStart() {
		if (range != null && range.contains("-")) {
			this.rangeStart = range.split("-")[0];
	    }
	 }
}
