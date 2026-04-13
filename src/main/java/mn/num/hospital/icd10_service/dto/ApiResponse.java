package mn.num.hospital.icd10_service.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        Object meta,
        String message,
        List<ApiError> errors,
        Instant timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null, null, null);
    }

    public static <T> ApiResponse<T> success(T data, Object meta) {
        return new ApiResponse<>(true, data, meta, null, null, null);
    }

    public static <T> ApiResponse<T> error(String message, List<ApiError> errors) {
        return new ApiResponse<>(false, null, null, message, errors, Instant.now());
    }
}
