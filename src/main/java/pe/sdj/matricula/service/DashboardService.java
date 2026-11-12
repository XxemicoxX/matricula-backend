package pe.sdj.matricula.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sdj.matricula.config.AppProperties;
import pe.sdj.matricula.domain.EstadoMatricula;
import pe.sdj.matricula.domain.EstadoSeccion;
import pe.sdj.matricula.domain.Seccion;
import pe.sdj.matricula.dto.AdminDtos.DashboardDto;
import pe.sdj.matricula.dto.AdminDtos.OcupacionSeccionDto;
import pe.sdj.matricula.repository.MatriculaRepository;
import pe.sdj.matricula.repository.SeccionRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/** CU-20: indicadores de aforo en tiempo real (RN-03: aforo = matriculados + reservas vigentes). */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SeccionRepository secciones;
    private final MatriculaRepository matriculas;
    private final AppProperties props;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardDto aforo() {
        String periodo = props.getPeriodoActual();
        Instant ahora = clock.instant();

        Map<Long, long[]> conteo = new HashMap<>(); // [matriculados, reservas]
        for (Object[] fila : matriculas.contarOcupadasPorSeccion(periodo, ahora)) {
            long[] c = conteo.computeIfAbsent((Long) fila[0], k -> new long[2]);
            long n = ((Number) fila[2]).longValue();
            if (fila[1] == EstadoMatricula.MATRICULADO) c[0] += n; else c[1] += n;
        }

        List<OcupacionSeccionDto> filas = new ArrayList<>();
        long aforoTotal = 0, matriculados = 0, reservas = 0, abiertas = 0;
        for (Seccion s : secciones.listar(null)) {
            long[] c = conteo.getOrDefault(s.getId(), new long[2]);
            long vacantes = Math.max(0, s.getAforoMaximo() - c[0] - c[1]);
            double pct = s.getAforoMaximo() == 0 ? 0
                    : Math.round(((c[0] + c[1]) * 1000.0 / s.getAforoMaximo())) / 10.0;
            filas.add(new OcupacionSeccionDto(s.getId(), s.getGrado().getNombre(), s.getGrado().getNivel(), s.getNombre(),
                    s.getAforoMaximo(), c[0], c[1], vacantes, s.getEstado().name(), pct));
            aforoTotal += s.getAforoMaximo();
            matriculados += c[0];
            reservas += c[1];
            if (s.getEstado() == EstadoSeccion.ABIERTA) abiertas++;
        }

        ZoneId zona = ZoneId.of(props.getZonaHoraria());
        Instant inicioHoy = LocalDate.now(clock.withZone(zona)).atStartOfDay(zona).toInstant();
        long hoy = matriculas.countByEstadoAndPeriodoAcademicoAndFechaMatriculaGreaterThanEqualAndFechaMatriculaLessThan(
                EstadoMatricula.MATRICULADO, periodo, inicioHoy, inicioHoy.plusSeconds(86400));

        return new DashboardDto(periodo, aforoTotal, matriculados, reservas,
                Math.max(0, aforoTotal - matriculados - reservas), abiertas, filas.size(), hoy, filas);
    }
}
