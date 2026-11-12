package pe.sdj.matricula.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sdj.matricula.common.BusinessException;
import pe.sdj.matricula.config.AppProperties;
import pe.sdj.matricula.domain.*;
import pe.sdj.matricula.dto.SeccionDtos.CrearSeccionRequest;
import pe.sdj.matricula.dto.SeccionDtos.ModificarSeccionRequest;
import pe.sdj.matricula.dto.SeccionDtos.SeccionDto;
import pe.sdj.matricula.repository.EstudianteRepository;
import pe.sdj.matricula.repository.GradoRepository;
import pe.sdj.matricula.repository.MatriculaRepository;
import pe.sdj.matricula.repository.SeccionRepository;
import pe.sdj.matricula.security.UsuarioActual;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SeccionService {

    private final SeccionRepository secciones;
    private final GradoRepository grados;
    private final MatriculaRepository matriculas;
    private final EstudianteRepository estudiantes;
    private final ReglasMatriculaService reglas;
    private final AuditoriaService auditoria;
    private final AppProperties props;
    private final Clock clock;

    /**
     * CU-6. Apoderado: requiere estudianteId; solo ve las secciones abiertas del grado del estudiante y
     * únicamente si cumple las reglas previas (sin deudas, validado en MINEDU, sin caso excepcional pendiente).
     * Personal: ve todas las secciones (con filtro opcional de grado).
     */
    @Transactional
    public List<SeccionDto> listar(UsuarioActual u, Long estudianteId, Long gradoId) {
        boolean apoderado = u.esApoderado();
        Long gradoFiltro = gradoId;

        if (apoderado) {
            if (estudianteId == null) {
                throw BusinessException.badRequest("SOLICITUD_INVALIDA", "Indica el estudiante para consultar las secciones.");
            }
            Estudiante e = estudiantes.findById(estudianteId)
                    .orElseThrow(() -> BusinessException.notFound("El estudiante no existe."));
            MatriculaService.verificarPropietario(e, u);
            reglas.validarElegibilidad(e);
            if (e.getGradoDestino() == null) {
                throw BusinessException.unprocessable("GRADO_NO_ASIGNADO",
                        "El estudiante aún no tiene un grado asignado para este periodo. Comunícate con Secretaría.");
            }
            gradoFiltro = e.getGradoDestino().getId();
        }

        Map<Long, long[]> uso = usoPorSeccion(); // [matriculados, reservados]
        return secciones.listar(gradoFiltro).stream()
                .filter(s -> !apoderado || s.getEstado() == EstadoSeccion.ABIERTA)
                .map(s -> aDto(s, uso.getOrDefault(s.getId(), new long[]{0, 0})))
                .toList();
    }

    @Transactional
    public SeccionDto crear(CrearSeccionRequest req) {
        Grado g = grados.findById(req.gradoId()).orElseThrow(() -> BusinessException.notFound("El grado no existe."));
        String nombre = req.nombre().trim().toUpperCase();
        if (secciones.existsByGradoIdAndNombreIgnoreCase(g.getId(), nombre)) {
            throw BusinessException.conflict("SECCION_DUPLICADA", "Ya existe una sección con ese nombre en el grado.");
        }
        Seccion s = new Seccion();
        s.setGrado(g);
        s.setNombre(nombre);
        s.setAforoMaximo(req.aforoMaximo());
        s.setAula(req.aula());
        s.setDocenteResponsable(req.docenteResponsable());
        s.setEstado(EstadoSeccion.ABIERTA);
        s = secciones.save(s);
        auditoria.registrar("CREAR_SECCION", "Sección " + s.getNombre() + " (" + g.getNivel() + " " + g.getNombre()
                + ") aforo " + s.getAforoMaximo());
        return aDto(s, new long[]{0, 0});
    }

    /** CU-23/CU-24: cambia aforo o abre/cierra. Cerrar no afecta reservas ya generadas (RN-08). */
    @Transactional
    public SeccionDto modificar(Long id, ModificarSeccionRequest req) {
        if (req.aforoMaximo() == null && req.estado() == null && req.aula() == null && req.docenteResponsable() == null) {
            throw BusinessException.badRequest("SOLICITUD_INVALIDA", "Indica al menos un dato a modificar.");
        }
        Seccion s = secciones.findByIdForUpdate(id).orElseThrow(() -> BusinessException.notFound("La sección no existe."));
        long[] uso = usoPorSeccion().getOrDefault(s.getId(), new long[]{0, 0});

        if (req.aforoMaximo() != null) {
            if (req.aforoMaximo() < uso[0] + uso[1]) {
                throw BusinessException.conflict("AFORO_MENOR_A_OCUPACION",
                        "El aforo no puede ser menor a los cupos ya ocupados o reservados (" + (uso[0] + uso[1]) + ").");
            }
            s.setAforoMaximo(req.aforoMaximo());
        }
        if (req.estado() != null) s.setEstado(req.estado());
        if (req.aula() != null) s.setAula(req.aula());
        if (req.docenteResponsable() != null) s.setDocenteResponsable(req.docenteResponsable());

        auditoria.registrar("MODIFICAR_SECCION", "Sección " + s.getId() + ": aforo=" + s.getAforoMaximo()
                + ", estado=" + s.getEstado());
        return aDto(s, uso);
    }

    private Map<Long, long[]> usoPorSeccion() {
        Map<Long, long[]> uso = new HashMap<>();
        for (Object[] fila : matriculas.contarOcupadasPorSeccion(props.getPeriodoActual(), clock.instant())) {
            long[] v = uso.computeIfAbsent((Long) fila[0], k -> new long[2]);
            long cantidad = ((Number) fila[2]).longValue();
            if (fila[1] == EstadoMatricula.MATRICULADO) v[0] += cantidad; else v[1] += cantidad;
        }
        return uso;
    }

    private SeccionDto aDto(Seccion s, long[] uso) {
        long vacantes = Math.max(0, s.getAforoMaximo() - uso[0] - uso[1]);
        Grado g = s.getGrado();
        return new SeccionDto(s.getId(), g.getId(), g.getNombre(), g.getNivel(), s.getNombre(), s.getAula(),
                s.getDocenteResponsable(), s.getAforoMaximo(), uso[0], uso[1], vacantes, s.getEstado(),
                s.getEstado() == EstadoSeccion.ABIERTA && vacantes > 0);
    }
}
