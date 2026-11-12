package pe.sdj.matricula.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.sdj.matricula.domain.EstadoSolicitud;
import pe.sdj.matricula.domain.MotivoExcepcion;
import pe.sdj.matricula.domain.TipoSolicitud;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class SolicitudDtos {

    private SolicitudDtos() {
    }

    /** Parte JSON "solicitud" de POST /solicitudes-modificacion (multipart). campos: dni, nombres, apellidos, fechaNacimiento, genero. */
    public record CrearSolicitudRequest(
            @NotNull Long estudianteId,
            @NotBlank @Size(max = 2000) String justificacion,
            @NotNull Map<String, String> campos) {
    }

    /** Parte JSON "solicitud" de POST /casos-excepcionales (multipart). */
    public record CrearCasoRequest(
            @NotNull Long estudianteId,
            @NotNull MotivoExcepcion motivo,
            @NotBlank @Size(max = 2000) String descripcion) {
    }

    public enum Decision { APROBAR, RECHAZAR }

    public record ResolverRequest(@NotNull Decision decision, @Size(max = 2000) String observacion) {
    }

    public record DocumentoDto(Long id, String nombreArchivo, String tipoDocumento, Instant fechaSubida) {
    }

    public record SolicitudDto(Long id, Long estudianteId, String estudianteNombre, String estudianteDni,
                               TipoSolicitud tipo, String motivo, String descripcion,
                               Map<String, String> datosSolicitados, EstadoSolicitud estado,
                               Instant fechaSolicitud, Instant fechaRespuesta, String observacion,
                               List<DocumentoDto> documentos) {
    }
}
