package pe.sdj.matricula.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envoltorio estándar de respuestas (documento 11.4): data / meta / error.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(T data, Map<String, Object> meta, ErrorBody error) {

    public record ErrorBody(String codigo, String mensaje, List<String> detalles) {
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, baseMeta(), null);
    }

    public static <T> ApiResponse<List<T>> page(Page<T> page) {
        Map<String, Object> meta = baseMeta();
        meta.put("page", page.getNumber());
        meta.put("size", page.getSize());
        meta.put("totalElements", page.getTotalElements());
        meta.put("totalPages", page.getTotalPages());
        return new ApiResponse<>(page.getContent(), meta, null);
    }

    public static ApiResponse<Void> error(String codigo, String mensaje, List<String> detalles) {
        return new ApiResponse<>(null, baseMeta(), new ErrorBody(codigo, mensaje, detalles));
    }

    private static Map<String, Object> baseMeta() {
        Map<String, Object> meta = new LinkedHashMap<>();
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId != null) {
            meta.put("requestId", requestId);
        }
        return meta;
    }
}
