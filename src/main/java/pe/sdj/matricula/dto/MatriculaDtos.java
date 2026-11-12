package pe.sdj.matricula.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.sdj.matricula.domain.EstadoMatricula;

import java.math.BigDecimal;
import java.time.Instant;

public final class MatriculaDtos {

    private MatriculaDtos() {
    }

    /** Cuerpo de POST /matriculas (documento 11.3). */
    public record ReservaRequest(@NotNull Long estudianteId, @NotNull Long seccionId, @NotBlank String periodoAcademico) {
    }

    public record MatriculaDto(Long matriculaId, Long estudianteId, String estudianteNombre,
                               Long seccionId, String seccionNombre, String gradoNombre, String nivel,
                               String periodoAcademico, EstadoMatricula estado, String codigoPago,
                               @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal monto,
                               Instant fechaReserva, Instant fechaExpiracion, Instant fechaMatricula,
                               boolean constanciaDisponible) {
    }
}
