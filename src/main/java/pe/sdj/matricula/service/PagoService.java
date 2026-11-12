package pe.sdj.matricula.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sdj.matricula.common.BusinessException;
import pe.sdj.matricula.config.AppProperties;
import pe.sdj.matricula.domain.*;
import pe.sdj.matricula.dto.PagoDtos.PagoManualRequest;
import pe.sdj.matricula.repository.*;
import pe.sdj.matricula.security.UsuarioActual;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Confirmación de pagos. Una sola ruta de negocio para el webhook del banco y el pago manual de contingencia:
 * - RN-07: la matrícula solo se confirma con el monto exacto.
 * - RN-09: un pago sobre una reserva vencida no confirma automáticamente; se avisa a Tesorería/Secretaría.
 * - Idempotencia: (proveedor, numero_operacion) nunca se aplica dos veces.
 */
@Service
@RequiredArgsConstructor
public class PagoService {

    public enum Resultado {CONFIRMADO, YA_REGISTRADO, TARDIO, MONTO_INCORRECTO, DUPLICADO, REUBICACION_REQUERIDA}

    public record PagoEntrante(String proveedor, String numeroOperacion, BigDecimal monto, Instant fechaPago,
                               String metodoPago, TipoPago tipo, Long usuarioId, String entidadFinanciera,
                               String referenciaComprobante) {
    }

    public record ResultadoPago(Resultado resultado, EstadoMatricula estadoMatricula, Long pagoId) {
    }

    private final MatriculaRepository matriculas;
    private final CodigoPagoRepository codigos;
    private final PagoRepository pagos;
    private final SeccionRepository secciones;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;
    private final NotificacionService notificaciones;
    private final AppProperties props;
    private final Clock clock;

    /** CU-17: registro manual por Tesorería/Secretaría ante fallas de comunicación bancaria. */
    @Transactional
    public ResultadoPago registrarManual(UsuarioActual u, PagoManualRequest req) {
        Matricula m = matriculas.findById(req.matriculaId())
                .orElseThrow(() -> BusinessException.notFound("La matrícula no existe."));
        CodigoPago cp = codigos.findByMatriculaId(m.getId())
                .orElseThrow(() -> BusinessException.notFound("La matrícula no tiene un código de pago."));

        ResultadoPago r = aplicar(cp, new PagoEntrante("MANUAL", req.numeroOperacion().trim(), req.monto(),
                req.fechaPago(), req.entidadFinanciera().trim(), TipoPago.MANUAL, u.id(),
                req.entidadFinanciera().trim(), req.referenciaComprobante()));

        switch (r.resultado()) {
            case MONTO_INCORRECTO -> throw BusinessException.unprocessable("MONTO_INCORRECTO",
                    "El monto no coincide con el monto exacto de la matrícula (S/ " + cp.getMonto().toPlainString() + ").");
            case YA_REGISTRADO -> throw BusinessException.conflict("PAGO_DUPLICADO",
                    "Ya existe un pago registrado con ese número de operación.");
            case DUPLICADO -> throw BusinessException.conflict("MATRICULA_YA_CONFIRMADA",
                    "La matrícula ya está confirmada; no se registró el pago adicional.");
            case REUBICACION_REQUERIDA -> throw BusinessException.conflict("REUBICACION_REQUERIDA",
                    "La reserva venció y la sección ya no tiene cupo. Reubica al estudiante o gestiona la devolución.");
            default -> {
            }
        }
        return r;
    }

    /**
     * Aplica un pago a un código de pago. Siempre serializa por matrícula (bloqueo de fila), por lo que dos
     * confirmaciones simultáneas del mismo pago nunca duplican el efecto.
     */
    @Transactional
    public ResultadoPago aplicar(CodigoPago cp, PagoEntrante in) {
        Matricula m = matriculas.findByIdForUpdate(cp.getMatricula().getId()).orElseThrow();
        Instant ahora = clock.instant();

        Optional<Pago> previo = pagos.findByProveedorAndNumeroOperacion(in.proveedor(), in.numeroOperacion());
        if (previo.isPresent()) {
            return new ResultadoPago(Resultado.YA_REGISTRADO, m.getEstado(), previo.get().getId());
        }

        if (m.getEstado() == EstadoMatricula.MATRICULADO) {
            Pago p = guardar(cp, in, EstadoPago.DUPLICADO, ahora);
            avisarPersonal(m, "PAGO_DUPLICADO", "Se recibió un pago adicional (op. " + in.numeroOperacion()
                    + ") para una matrícula ya confirmada. Gestionar devolución.");
            return new ResultadoPago(Resultado.DUPLICADO, m.getEstado(), p.getId());
        }

        if (cp.getMonto().compareTo(in.monto()) != 0) {
            Pago p = guardar(cp, in, EstadoPago.RECHAZADO_MONTO, ahora);
            avisarPersonal(m, "PAGO_MONTO_INCORRECTO", "Pago op. " + in.numeroOperacion() + " por S/ "
                    + in.monto().toPlainString() + " no coincide con el monto exigido S/ " + cp.getMonto().toPlainString()
                    + " (código " + cp.getCodigo() + ").");
            return new ResultadoPago(Resultado.MONTO_INCORRECTO, m.getEstado(), p.getId());
        }

        boolean vencida = m.getEstado() == EstadoMatricula.EXPIRADA || !m.getFechaExpiracion().isAfter(ahora);
        if (vencida) {
            boolean recuperada = in.tipo() == TipoPago.MANUAL && reactivar(m, cp, ahora);
            if (!recuperada) {
                Pago p = guardar(cp, in, EstadoPago.TARDIO, ahora);
                avisarPersonal(m, "PAGO_TARDIO", "Pago op. " + in.numeroOperacion() + " recibido después de vencida la reserva "
                        + "(código " + cp.getCodigo() + "). Reubicar al estudiante o gestionar la devolución.");
                return new ResultadoPago(in.tipo() == TipoPago.MANUAL ? Resultado.REUBICACION_REQUERIDA : Resultado.TARDIO,
                        m.getEstado(), p.getId());
            }
        }

        Pago p = guardar(cp, in, EstadoPago.CONFIRMADO, ahora);
        m.setEstado(EstadoMatricula.MATRICULADO);
        m.setFechaMatricula(ahora);
        cp.setEstado(EstadoCodigoPago.PAGADO);

        auditoria.registrarComo(in.usuarioId(), in.tipo() == TipoPago.MANUAL ? "PAGO_MANUAL_CONFIRMADO" : "PAGO_CONFIRMADO",
                "Matrícula " + m.getId() + " confirmada con op. " + in.numeroOperacion() + " (" + in.proveedor() + ")");
        Estudiante e = m.getEstudiante();
        notificaciones.notificarApoderado(e.getApoderado(), m, "MATRICULA_CONFIRMADA",
                "¡Matrícula confirmada! " + e.nombreCompleto() + " quedó matriculado(a) para el año " + m.getPeriodoAcademico()
                        + ". Ya puedes descargar la constancia en PDF.");
        return new ResultadoPago(Resultado.CONFIRMADO, m.getEstado(), p.getId());
    }

    /** Pago manual sobre una reserva vencida: solo se recupera si la sección sigue abierta y con cupo. */
    private boolean reactivar(Matricula m, CodigoPago cp, Instant ahora) {
        Seccion s = secciones.findByIdForUpdate(m.getSeccion().getId()).orElseThrow();
        if (s.getEstado() != EstadoSeccion.ABIERTA
                || matriculas.contarOcupadas(s.getId(), m.getPeriodoAcademico(), ahora) >= s.getAforoMaximo()) {
            return false;
        }
        m.setEstado(EstadoMatricula.RESERVA_PENDIENTE);
        m.setFechaExpiracion(ahora);
        return true;
    }

    private Pago guardar(CodigoPago cp, PagoEntrante in, EstadoPago estado, Instant ahora) {
        Pago p = new Pago();
        p.setCodigoPago(cp);
        if (in.usuarioId() != null) {
            p.setUsuario(usuarios.getReferenceById(in.usuarioId()));
        }
        p.setProveedor(in.proveedor());
        p.setNumeroOperacion(in.numeroOperacion());
        p.setMonto(in.monto());
        p.setFechaPago(in.fechaPago() != null ? in.fechaPago() : ahora);
        p.setMetodoPago(in.metodoPago());
        p.setTipoPago(in.tipo());
        p.setEstado(estado);
        p.setEntidadFinanciera(in.entidadFinanciera());
        p.setReferenciaComprobante(in.referenciaComprobante());
        p.setFechaRegistro(ahora);
        return pagos.save(p);
    }

    private void avisarPersonal(Matricula m, String tipo, String mensaje) {
        notificaciones.notificarRol(Roles.TESORERIA, m, tipo, mensaje);
        notificaciones.notificarRol(Roles.SECRETARIA, m, tipo, mensaje);
    }
}
