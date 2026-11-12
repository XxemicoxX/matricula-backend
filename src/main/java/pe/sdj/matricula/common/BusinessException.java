package pe.sdj.matricula.common;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio con código HTTP y código de error estable para el Front-End.
 * Los mensajes son comprensibles para el usuario y no exponen detalles internos (documento 11.5).
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public BusinessException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public static BusinessException badRequest(String codigo, String mensaje) {
        return new BusinessException(HttpStatus.BAD_REQUEST, codigo, mensaje);
    }

    public static BusinessException unauthorized(String codigo, String mensaje) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, codigo, mensaje);
    }

    public static BusinessException forbidden(String codigo, String mensaje) {
        return new BusinessException(HttpStatus.FORBIDDEN, codigo, mensaje);
    }

    public static BusinessException notFound(String mensaje) {
        return new BusinessException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", mensaje);
    }

    public static BusinessException conflict(String codigo, String mensaje) {
        return new BusinessException(HttpStatus.CONFLICT, codigo, mensaje);
    }

    public static BusinessException unprocessable(String codigo, String mensaje) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, codigo, mensaje);
    }

    public static BusinessException unavailable(String codigo, String mensaje) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, codigo, mensaje);
    }

    public static BusinessException timeout(String codigo, String mensaje) {
        return new BusinessException(HttpStatus.GATEWAY_TIMEOUT, codigo, mensaje);
    }
}
