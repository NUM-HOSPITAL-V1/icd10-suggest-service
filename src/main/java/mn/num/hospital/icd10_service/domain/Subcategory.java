package mn.num.hospital.icd10_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="subcategories")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Subcategory {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	private String code; // A00
	
	@Column(columnDefinition = "TEXT")
	private String name;
	
	@ManyToOne
	@JoinColumn(name="category_id")
	private Category category;
}
