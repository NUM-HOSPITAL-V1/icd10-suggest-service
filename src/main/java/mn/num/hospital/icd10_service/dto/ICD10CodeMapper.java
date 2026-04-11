package mn.num.hospital.icd10_service.dto;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import mn.num.hospital.icd10_service.domain.Category;
import mn.num.hospital.icd10_service.domain.Chapter;
import mn.num.hospital.icd10_service.domain.ICD10Code;
import mn.num.hospital.icd10_service.domain.Subcategory;

public class ICD10CodeMapper {

	private ICD10CodeMapper() {}
	
	// Entity -> DTO
	public static ICD10CodeDTO toDto(ICD10Code entity) {
		if(entity == null) return null; 
		
		ICD10CodeDTO dto = new ICD10CodeDTO();
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setDetail(entity.getDetail());
        
        Chapter chapter = entity.getChapter();
        if(chapter != null) {
        	dto.setChapterCode(chapter.getChapter());
        	dto.setChapterName(chapter.getName());
        }

        Category category = entity.getCategory();
        if(category != null) {
        	dto.setCategoryRange(category.getRange());
        	dto.setCategoryName(category.getName());
        }
        
        Subcategory subcategory = entity.getSubcategory();
        if(subcategory != null) {
        	dto.setSubcategoryCode(subcategory.getCode());
        	dto.setSubcategoryName(subcategory.getName());
        }
        return dto;
	}
	
    // DTO -> Entity
    // Тайлбар: chapter, category, subcategory нь FK тул
    // mapper дотор шийдэх боломжгүй — service layer-т repo ашиглан lookup хийх шаардлагатай.
    // Энд зөвхөн code/name/detail-ийг map хийнэ.	
	public static ICD10Code toEntity(ICD10CodeDTO dto) {
		if(dto == null) return null;
		
		ICD10Code entity = new ICD10Code();
		entity.setCode(dto.getCode() != null ? dto.getCode().trim() : null);
        entity.setName(dto.getName() != null ? dto.getName().trim() : null);
        entity.setDetail(dto.getDetail() != null ? dto.getDetail().trim() : "");
        
        // chapter, category, subcategory — service layer-т set хийнэ
		return entity;
	}
	
	public static List<ICD10CodeDTO> toDtoList(List<ICD10Code> entities){
		if(entities == null) return null; 
		
		return entities.stream()
				.map(ICD10CodeMapper::toDto)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}
	
	public static List<ICD10Code> toEntityList(List<ICD10CodeDTO> dtos){
		if(dtos == null) return null;
		
		return dtos.stream()
                .map(ICD10CodeMapper::toEntity)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
	}
}