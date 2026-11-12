package pe.sdj.matricula.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> negocio(BusinessException ex) {
        return respuesta(ex.getStatus(), ex.getCodigo(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", MSG_400, detalles);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> restricciones(ConstraintViolationException ex) {
        List<String> detalles = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage()).toList();
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", MSG_400, detalles);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestValueException.class,
            MethodArgumentTypeMismatchException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ApiResponse<Void>> malFormada(Exception ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", MSG_400, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> archivoGrande(MaxUploadSizeExceededException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "ARCHIVO_MUY_GRANDE",
                "El archivo supera el tamaño máximo permitido (5 MB).", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> metodo(HttpRequestMethodNotSupportedException ex) {
        return respuesta(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NO_PERMITIDO", "Operación no permitida.", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> sinRecurso(NoResourceFoundException ex) {
        return respuesta(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", MSG_404, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> noAutenticado(AuthenticationException ex) {
        return respuesta(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", MSG_401, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> prohibido(AccessDeniedException ex) {
        return respuesta(HttpStatus.FORBIDDEN, "SIN_PERMISOS", MSG_403, null);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> bloqueo(PessimisticLockingFailureException ex) {
        log.warn("Conflicto de concurrencia: {}", ex.getClass().getSimpleName());
        return respuesta(HttpStatus.CONFLICT, "CONFLICTO_CONCURRENCIA",
                "Hay mucha demanda en este momento. Por favor, inténtalo nuevamente.", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> integridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return respuesta(HttpStatus.CONFLICT, "CONFLICTO_DATOS",
                "La operación entra en conflicto con un registro existente.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> inesperado(Exception ex) {
        log.error("Error inesperado", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", MSG_500, null);
    }

    private ResponseEntity<ApiResponse<Void>> respuesta(HttpStatus status, String codigo, String mensaje, List<String> detalles) {
        return ResponseEntity.status(status).body(ApiResponse.error(codigo, mensaje, detalles));
    }

    public static final String MSG_400 = "La información enviada es incorrecta. Por favor, revisa los datos e inténtalo de nuevo.";
    public static final String MSG_401 = "Tu sesión ha expirado o no has iniciado sesión. Por favor, ingresa nuevamente mediante CUBICOL.";
    public static final String MSG_403 = "No tienes los permisos necesarios para acceder a esta opción.";
    public static final String MSG_404 = "La información que buscas no se encuentra disponible o no existe.";
    public static final String MSG_500 = "Ocurrió un error inesperado en nuestro sistema. Por favor, inténtalo más tarde.";
}
