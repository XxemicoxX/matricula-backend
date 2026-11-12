package pe.sdj.matricula.dto;

import pe.sdj.matricula.domain.EstadoMatricula;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record OcupacionSeccionDto(Long seccionId, String gradoNombre, String nivel, String seccionNombre,
                                      int aforoMaximo, long matriculados, long reservasPendientes, long vacantes,
                                      String estado, double porcentajeOcupacion) {
    }

    /** Indicadores del dashboard de aforo (CU-20). */
    public record DashboardDto(String periodo, long aforoTotal, long matriculados, long reservasPendientes,
                               long vacantesDisponibles, long seccionesAbiertas, long seccionesTotal,
                               long matriculadosHoy, List<OcupacionSeccionDto> porSeccion) {
    }

    public record FilaReporteDto(Long matriculaId, String estudiante, String dni, String grado, String seccion,
                                 LocalDate fecha, EstadoMatricula estado) {
    }

    public record AuditoriaDto(Long id, Long usuarioId, String usuario, String accion, Instant fechaHora,
                               String direccionIp, String descripcion) {
    }

    public record NotificacionDto(Long id, String tipo, String canal, String mensaje, Instant fechaEnvio,
                                  boolean leida, Long matriculaId) {
    }
}
