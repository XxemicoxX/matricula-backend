package pe.sdj.matricula.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.sdj.matricula.domain.CondicionEstudiante;
import pe.sdj.matricula.domain.EstadoDeuda;
import pe.sdj.matricula.domain.ResultadoMinedu;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class EstudianteDtos {

    private EstudianteDtos() {
    }

    public record ApoderadoDto(Long id, String dni, String nombres, String apellidos,
                               String telefono, String direccion, String correo) {
    }

    public record GradoDto(Long id, String nombre, String nivel,
                           @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal montoMatricula) {
    }

    public record EstudianteDto(Long id, String dni, String nombres, String apellidos, LocalDate fechaNacimiento,
                                String genero, String correo, String telefono, String direccion,
                                CondicionEstudiante condicion, boolean excepcionAprobada,
                                GradoDto gradoDestino, ApoderadoDto apoderado, MatriculaDtos.MatriculaDto matriculaActual) {
    }

    /** Todos los campos son opcionales, pero debe venir al menos uno. */
    public record ContactoRequest(
            @Pattern(regexp = "^[0-9+\\- ]{6,20}$", message = "teléfono inválido") String telefono,
            @Size(max = 200) String direccion,
            @Email @Size(max = 150) String correo) {
    }

    public record DeudaDto(String periodo, String concepto,
                           @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal monto, EstadoDeuda estado) {
    }

    public record ResultadoDeudasDto(boolean puedeContinuar, String periodoConsultado,
                                     @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalPendiente,
                                     List<DeudaDto> deudas) {
    }

    public record ValidacionMineduDto(ResultadoMinedu resultado, String observacion, Instant fecha) {
    }
}
