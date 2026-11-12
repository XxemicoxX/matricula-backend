package pe.sdj.matricula.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sdj.matricula.common.BusinessException;
import pe.sdj.matricula.config.AppProperties;
import pe.sdj.matricula.domain.*;
import pe.sdj.matricula.dto.MatriculaDtos.MatriculaDto;
import pe.sdj.matricula.dto.MatriculaDtos.ReservaRequest;
import pe.sdj.matricula.repository.*;
import pe.sdj.matricula.security.UsuarioActual;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Reserva de vacante + generación del código de pago (CU-8, CU-9, CU-10), consulta de estado
 * y constancia. Reglas: RN-01, RN-02, RN-03, RN-06, RN-08, RN-10.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatriculaService {

    public record Constancia(String nombreArchivo, byte[] contenido) {
    }

    private final MatriculaRepository matriculas;
    private final CodigoPagoRepository codigos;
    private final EstudianteRepository estudiantes;
    private final SeccionRepository secciones;
    private final PagoRepository pagos;
    private final ReglasMatriculaService reglas;
    private final AuditoriaService auditoria;
    private final NotificacionService notificaciones;
    private final PdfConstanciaService pdf;
    private final AppProperties props;
    private final Clock clock;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public MatriculaDto reservar(UsuarioActual u, ReservaRequest req) {
        // Orden de bloqueos: estudiante -> sección (siempre igual, evita interbloqueos).
        Estudiante e = estudiantes.findByIdForUpdate(req.estudianteId())
                .orElseThrow(() -> BusinessException.notFound("El estudiante no existe."));
        verificarPropietario(e, u);

        String periodo = props.getPeriodoActual();
        if (!periodo.equals(req.periodoAcademico())) {
            throw BusinessException.unprocessable("PERIODO_NO_VIGENTE",
                    "El periodo académico indicado no está habilitado para matrícula.");
        }

        reglas.validarElegibilidad(e); // RN-10, RN-01, RN-06

        Instant ahora = clock.instant();
        if (matriculas.findFirstByEstudianteIdAndPeriodoAcademicoAndEstadoOrderByIdDesc(
                e.getId(), periodo, EstadoMatricula.MATRICULADO).isPresent()) {
            throw BusinessException.conflict("YA_MATRICULADO", "El estudiante ya se encuentra matriculado en este periodo.");
        }
        Optional<Matricula> pendiente = matriculas.findFirstByEstudianteIdAndPeriodoAcademicoAndEstadoOrderByIdDesc(
                e.getId(), periodo, EstadoMatricula.RESERVA_PENDIENTE);
        if (pendiente.isPresent()) {
            if (pendiente.get().getFechaExpiracion().isAfter(ahora)) {
                throw BusinessException.conflict("RESERVA_ACTIVA",
                        "La sección seleccionada ya no tiene vacantes o ya cuentas con una reserva activa.");
            }
            expirar(pendiente.get()); // vencida y aún no barrida por el proceso automático
        }

        Seccion s = secciones.findByIdForUpdate(req.seccionId())
                .orElseThrow(() -> BusinessException.notFound("La sección no existe."));
        if (s.getEstado() != EstadoSeccion.ABIERTA) {
            throw BusinessException.conflict("SECCION_CERRADA",
                    "La sección seleccionada ya no tiene vacantes o está cerrada para nuevas reservas.");
        }
        if (e.getGradoDestino() == null || !e.getGradoDestino().getId().equals(s.getGrado().getId())) {
            throw BusinessException.unprocessable("GRADO_NO_CORRESPONDE",
                    "La sección elegida no corresponde al grado del estudiante.");
        }
        // RN-03: aforo = matriculados + reservas vigentes; se cuenta con la sección bloqueada.
        if (matriculas.contarOcupadas(s.getId(), periodo, ahora) >= s.getAforoMaximo()) {
            throw BusinessException.conflict("AFORO_AGOTADO",
                    "La sección seleccionada ya no tiene vacantes o ya cuentas con una reserva activa.");
        }

        Instant vence = ahora.plus(Duration.ofHours(props.getMatricula().getReservaHoras())); // RN-02
        Matricula m = new Matricula();
        m.setEstudiante(e);
        m.setSeccion(s);
        m.setPeriodoAcademico(periodo);
        m.setEstado(EstadoMatricula.RESERVA_PENDIENTE);
        m.setFechaReserva(ahora);
        m.setFechaExpiracion(vence);
        m = matriculas.save(m);

        CodigoPago cp = new CodigoPago();
        cp.setMatricula(m);
        cp.setCodigo(generarCodigo(periodo, e.getId()));
        cp.setMonto(s.getGrado().getMontoMatricula() != null ? s.getGrado().getMontoMatricula()
                : props.getMatricula().getMontoDefecto());
        cp.setFechaGeneracion(ahora);
        cp.setFechaExpiracion(vence);
        cp.setEstado(EstadoCodigoPago.GENERADO);
        cp = codigos.save(cp);

        auditoria.registrar("RESERVA_VACANTE", "Reserva " + m.getId() + " estudiante " + e.getId()
                + " sección " + s.getId() + " código " + cp.getCodigo());
        notificaciones.notificarApoderado(e.getApoderado(), m, "RESERVA_CREADA",
                "Reservamos una vacante para " + e.nombreCompleto() + " en la sección " + s.getNombre()
                        + ". Realiza el pago con el código " + cp.getCodigo() + " antes del "
                        + vence + " (UTC) para confirmar la matrícula.");
        return aDto(m, cp);
    }

    @Transactional(readOnly = true)
    public MatriculaDto obtener(Long id, UsuarioActual u) {
        return aDto(cargarAutorizada(id, u), null);
    }

    @Transactional(readOnly = true)
    public List<MatriculaDto> listarPorEstudiante(Long estudianteId, UsuarioActual u) {
        Estudiante e = estudiantes.findById(estudianteId)
                .orElseThrow(() -> BusinessException.notFound("El estudiante no existe."));
        verificarPropietario(e, u);
        return matriculas.findByEstudianteIdOrderByIdDesc(estudianteId).stream().map(m -> aDto(m, null)).toList();
    }

    /** Matrícula vigente (confirmada o con reserva activa) del periodo actual, para mostrarla en la ficha del estudiante. */
    @Transactional(readOnly = true)
    public MatriculaDto vigente(Long estudianteId) {
        String periodo = props.getPeriodoActual();
        Instant ahora = clock.instant();
        return matriculas.findFirstByEstudianteIdAndPeriodoAcademicoAndEstadoOrderByIdDesc(
                        estudianteId, periodo, EstadoMatricula.MATRICULADO)
                .or(() -> matriculas.findFirstByEstudianteIdAndPeriodoAcademicoAndEstadoOrderByIdDesc(
                                estudianteId, periodo, EstadoMatricula.RESERVA_PENDIENTE)
                        .filter(m -> m.getFechaExpiracion().isAfter(ahora)))
                .map(m -> aDto(m, null)).orElse(null);
    }

    /** CU-12: solo disponible cuando la matrícula está confirmada (RN-07). */
    @Transactional(readOnly = true)
    public Constancia constancia(Long id, UsuarioActual u) {
        Matricula m = cargarAutorizada(id, u);
        if (m.getEstado() != EstadoMatricula.MATRICULADO) {
            throw BusinessException.conflict("CONSTANCIA_NO_DISPONIBLE",
                    "La constancia estará disponible cuando se confirme el pago de la matrícula.");
        }
        CodigoPago cp = codigos.findByMatriculaId(m.getId()).orElseThrow();
        Pago pago = pagos.findFirstByCodigoPagoIdAndEstadoOrderByIdDesc(cp.getId(), EstadoPago.CONFIRMADO).orElse(null);
        byte[] pdfBytes = pdf.generar(m, cp, pago);
        auditoria.registrar("DESCARGA_CONSTANCIA", "Constancia de la matrícula " + m.getId());
        return new Constancia("constancia-matricula-" + m.getId() + ".pdf", pdfBytes);
    }

    /** Libera las reservas vencidas (RN-02). Lo invoca el barrido programado; es idempotente. */
    @Transactional
    public int expirarVencidas() {
        Instant ahora = clock.instant();
        List<Matricula> vencidas = matriculas.findTop200ByEstadoAndFechaExpiracionLessThanEqual(
                EstadoMatricula.RESERVA_PENDIENTE, ahora);
        vencidas.forEach(this::expirar);
        if (!vencidas.isEmpty()) {
            auditoria.registrar("RESERVAS_EXPIRADAS", vencidas.size() + " reserva(s) liberada(s) por vencimiento de 48 h");
        }
        return vencidas.size();
    }

    private void expirar(Matricula m) {
        m.setEstado(EstadoMatricula.EXPIRADA);
        codigos.findByMatriculaId(m.getId()).ifPresent(cp -> {
            if (cp.getEstado() == EstadoCodigoPago.GENERADO) {
                cp.setEstado(EstadoCodigoPago.EXPIRADO);
            }
        });
        notificaciones.notificarApoderado(m.getEstudiante().getApoderado(), m, "RESERVA_EXPIRADA",
                "La reserva de vacante de " + m.getEstudiante().nombreCompleto()
                        + " venció sin confirmación de pago y la vacante fue liberada.");
    }

    // ---- helpers ----

    private Matricula cargarAutorizada(Long id, UsuarioActual u) {
        Matricula m = matriculas.findById(id).orElseThrow(() -> BusinessException.notFound("La matrícula no existe."));
        verificarPropietario(m.getEstudiante(), u);
        return m;
    }

    /** El apoderado solo ve a sus estudiantes; para el resto se responde 404 (no se revela la existencia). */
    static void verificarPropietario(Estudiante e, UsuarioActual u) {
        if (u.esApoderado() && !e.getApoderado().getId().equals(u.apoderadoId())) {
            throw BusinessException.notFound("La información que buscas no se encuentra disponible o no existe.");
        }
    }

    private String generarCodigo(String periodo, Long estudianteId) {
        long intento = matriculas.countByEstudianteIdAndPeriodoAcademico(estudianteId, periodo);
        for (int i = 0; i < 8; i++) {
            String c = "S%s-%08d-%03d".formatted(periodo, random.nextInt(100_000_000), intento);
            if (!codigos.existsByCodigo(c)) {
                return c;
            }
        }
        throw new IllegalStateException("No se pudo generar un código de pago único");
    }

    MatriculaDto aDto(Matricula m, CodigoPago cp) {
        CodigoPago codigo = cp != null ? cp : codigos.findByMatriculaId(m.getId()).orElse(null);
        Estudiante e = m.getEstudiante();
        Seccion s = m.getSeccion();
        return new MatriculaDto(m.getId(), e.getId(), e.nombreCompleto(), s.getId(), s.getNombre(),
                s.getGrado().getNombre(), s.getGrado().getNivel(), m.getPeriodoAcademico(), m.getEstado(),
                codigo == null ? null : codigo.getCodigo(), codigo == null ? null : codigo.getMonto(),
                m.getFechaReserva(), m.getFechaExpiracion(), m.getFechaMatricula(),
                m.getEstado() == EstadoMatricula.MATRICULADO);
    }
}
