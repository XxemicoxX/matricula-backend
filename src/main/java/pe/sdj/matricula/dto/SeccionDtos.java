package pe.sdj.matricula.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.sdj.matricula.domain.EstadoSeccion;

public final class SeccionDtos {

    private SeccionDtos() {
    }

    /**
     * @param ocupadas   matriculados confirmados
     * @param reservadas reservas temporales vigentes (48 h)
     * @param vacantes   aforo - ocupadas - reservadas (RN-03)
     */
    public record SeccionDto(Long id, Long gradoId, String gradoNombre, String nivel, String nombre, String aula,
                             String docenteResponsable, int aforoMaximo, long ocupadas, long reservadas,
                             long vacantes, EstadoSeccion estado, boolean disponible) {
    }

    public record CrearSeccionRequest(
            @NotNull Long gradoId,
            @NotBlank @Size(max = 10) String nombre,
            @NotNull @Min(1) @Max(200) Integer aforoMaximo,
            @Size(max = 50) String aula,
            @Size(max = 150) String docenteResponsable) {
    }

    /** Todos opcionales; debe venir al menos uno. */
    public record ModificarSeccionRequest(
            @Min(1) @Max(200) Integer aforoMaximo,
            EstadoSeccion estado,
            @Size(max = 50) String aula,
            @Size(max = 150) String docenteResponsable) {
    }
}
