package pe.sdj.matricula.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.sdj.matricula.domain.*;
import pe.sdj.matricula.dto.PagoDtos.WebhookPago;
import pe.sdj.matricula.repository.CodigoPagoRepository;
import pe.sdj.matricula.repository.EventoPagoRepository;
import pe.sdj.matricula.service.PagoService.PagoEntrante;
import pe.sdj.matricula.service.PagoService.ResultadoPago;

import java.time.Clock;
import java.util.Optional;

/** Procesa una confirmación bancaria ya guardada en la bandeja. Cada evento va en su propia transacción. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PagoProcesador {

    private final EventoPagoRepository eventos;
    private final CodigoPagoRepository codigos;
    private final PagoService pagoService;
    private final ObjectMapper om;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void procesar(Long eventoId) throws Exception {
        EventoPago ev = eventos.findById(eventoId).orElse(null);
        if (ev == null || (ev.getEstado() != EstadoEventoPago.PENDIENTE && ev.getEstado() != EstadoEventoPago.ERROR)) {
            return;
        }
        ev.setIntentos(ev.getIntentos() + 1);
        WebhookPago w = om.readValue(ev.getPayload(), WebhookPago.class);

        if (w.estado() != null && !"PAGADO".equalsIgnoreCase(w.estado())) {
            cerrar(ev, EstadoEventoPago.IGNORADO, "Estado no confirmatorio: " + w.estado());
            return;
        }
        Optional<CodigoPago> cp = codigos.findByCodigo(w.codigoPago().trim());
        if (cp.isEmpty()) {
            log.warn("Webhook {} con código de pago desconocido (op. {})", ev.getProveedor(), w.numeroOperacion());
            cerrar(ev, EstadoEventoPago.IGNORADO, "CODIGO_DESCONOCIDO");
            return;
        }
        ResultadoPago r = pagoService.aplicar(cp.get(), new PagoEntrante(ev.getProveedor(), w.numeroOperacion().trim(),
                w.monto(), w.fechaPago(), w.metodoPago(), TipoPago.AUTOMATICO, null, null, null));
        cerrar(ev, EstadoEventoPago.PROCESADO, r.resultado().name());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarError(Long eventoId, String detalle) {
        eventos.findById(eventoId).ifPresent(ev -> {
            if (ev.getEstado() != EstadoEventoPago.PROCESADO) {
                ev.setIntentos(ev.getIntentos() + 1);
                ev.setEstado(EstadoEventoPago.ERROR);
                ev.setDetalle(detalle == null ? "Error" : detalle.substring(0, Math.min(480, detalle.length())));
            }
        });
    }

    private void cerrar(EventoPago ev, EstadoEventoPago estado, String detalle) {
        ev.setEstado(estado);
        ev.setDetalle(detalle);
        ev.setFechaProcesamiento(clock.instant());
    }
}
