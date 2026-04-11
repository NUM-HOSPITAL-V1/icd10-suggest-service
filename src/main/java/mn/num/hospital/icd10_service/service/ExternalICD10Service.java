package mn.num.hospital.icd10_service.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import mn.num.hospital.icd10_service.dto.ICD10CodeDTO;

@Service
@Slf4j
public class ExternalICD10Service {

    private static final double DEFAULT_RELEVANCE = 0.8;

    @Value("${icd10.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ExternalICD10Service(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    // API-с татаж, diagnosis-тэй тохирох кодуудыг буцаана
    public List<ICD10CodeDTO> fetchFromExternalAPI(String diagnosis, int limit) {

        log.info("External API calling... URL: {}", apiUrl);

        try {
            String response = restTemplate.getForObject(apiUrl, String.class);

            if (response == null || response.isEmpty()) {
                log.warn("External API returned empty response");
                return List.of();
            }

            // Бүх кодыг задлаж flat жагсаалт үүсгэнэ
            List<ICD10CodeDTO> allCodes = parseAllCodes(response);
            log.info("Total parsed codes from external API: {}", allCodes.size());

            // Diagnosis-тэй тохирохыг шүүж limit хэрэглэнэ
            return allCodes.stream()
                    .filter(dto -> isMatch(dto, diagnosis))
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("External API error: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    // Бүх кодыг татах (шүүлтгүй) — DB seed хийхэд ашиглана 
    public List<ICD10CodeDTO> fetchAllCodes() {
        log.info("Fetching ALL codes from external API...");
        try {
            String response = restTemplate.getForObject(apiUrl, String.class);
            if (response == null || response.isEmpty()) return List.of();
            return parseAllCodes(response);
        } catch (Exception e) {
            log.error("External API fetch all error: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    // JSON-г задалж flat ICD10CodeDTO жагсаалт болгоно 
    private List<ICD10CodeDTO> parseAllCodes(String response) throws Exception {
        List<ICD10CodeDTO> result = new ArrayList<>();
        JsonNode root = objectMapper.readTree(response);

        // { "chapters": [...] } эсвэл шууд [...] байж болно
        JsonNode chaptersNode = root.isArray() ? root : root.get("chapters");

        if (chaptersNode == null || !chaptersNode.isArray()) {
            log.warn("'chapters' array олдсонгүй");
            return result;
        }
        
        for (JsonNode chapterNode : chaptersNode) {
            String chapterCode = getText(chapterNode, "chapter");  // "I", "II" ...
            String chapterName = getText(chapterNode, "name");     // "Халдварт ба шимэгчит..."

            JsonNode categories = chapterNode.get("categories");
            if (categories == null || !categories.isArray()) continue;

            for (JsonNode categoryNode : categories) {
                String categoryRange = getText(categoryNode, "range"); // "A00-A09"
                String categoryName  = getText(categoryNode, "name");  // "Гэдэсний халдварт өвчин"

                JsonNode subcategories = categoryNode.get("subcategories");
                if (subcategories == null || !subcategories.isArray()) continue;

                for (JsonNode subNode : subcategories) {
                    String subCode = getText(subNode, "code"); // "A00"
                    String subName = getText(subNode, "name"); // "Урвах тахал"

                    JsonNode subcodes = subNode.get("subcode");
                    if (subcodes != null && subcodes.isArray() && subcodes.size() > 0) {
                        // Leaf node: A00.0, A00.1
                        for (JsonNode leaf : subcodes) {
                        	ICD10CodeDTO dto = buildDTO(
                        		    getText(leaf, "code"),
                        		    getText(leaf, "name"),
                        		    getText(leaf, "detail"),
                        		    chapterCode, chapterName,
                        		    categoryRange, categoryName,
                        		    subCode, subName
                        		);
                            if (dto != null) result.add(dto);
                        }
                    } else {
                        // subcode байхгүй бол subcategory өөрөө leaf болно (A00 гэх мэт)
                    	ICD10CodeDTO dto = buildDTO(
                    		    subCode, subName, "",
                    		    chapterCode, chapterName,
                    		    categoryRange, categoryName,
                    		    "", ""
                    	);
                        if (dto != null) result.add(dto);
                    }
                }
            }
        }
        
        return result;
    }
    
    // Dto үүсгэх 
    private ICD10CodeDTO buildDTO(String code, String name, String detail,
                               String chapterCode, String chapterName,
                               String categoryRange, String categoryName,
                               String subcategoryCode, String subcategoryName) {
    if (code == null || code.isBlank() || name == null || name.isBlank()) {
        return null;
    }
	    ICD10CodeDTO dto = new ICD10CodeDTO();
	    dto.setCode(code.trim());
	    dto.setName(name.trim());
	    dto.setDetail(detail != null ? detail.trim() : "");
	    dto.setChapterCode(chapterCode);
	    dto.setChapterName(chapterName);
	    dto.setCategoryRange(categoryRange);
	    dto.setCategoryName(categoryName);
	    dto.setSubcategoryCode(subcategoryCode);
	    dto.setSubcategoryName(subcategoryName);
	    dto.setRelevanceScore(DEFAULT_RELEVANCE);
	    return dto;
	}
    
    private String getText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asText().trim() : "";
    }

    private boolean isMatch(ICD10CodeDTO dto, String diagnosis) {
        if (diagnosis == null || diagnosis.isBlank()) return false;
        String keyword = diagnosis.toLowerCase();
        return dto.getName().toLowerCase().contains(keyword)
            || dto.getCode().toLowerCase().contains(keyword)
            || (dto.getDetail() != null && dto.getDetail().toLowerCase().contains(keyword));
    } 
}