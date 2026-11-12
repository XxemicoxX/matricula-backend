package pe.sdj.matricula.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import pe.sdj.matricula.common.BusinessException;
import pe.sdj.matricula.config.AppProperties;
import pe.sdj.matricula.domain.EstadoEventoPago;
import pe.sdj.matricula.domain.EventoPago;
import pe.sdj.matricula.dto.PagoDtos.WebhookPago;
import pe.sdj.matricula.repository.EventoPagoRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * CU-16: recepción de la confirmación del banco.
 * 1) Verifica la firma HMAC-SHA256 del cuerpo.  2) Guarda el evento de forma durable (idempotente por
 * proveedor + n.º de operación).  3) Lo procesa; si falla, queda en la bandeja y se reintenta
 * automáticamente (PagoReintentos), de modo que no se pierde ninguna confirmación.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PagoWebhookService {

    private final EventoPagoRepository eventos;
    private final PagoProcesador procesador;
    private final AppProperties props;
    private final ObjectMapper om;
    private final Clock clock;

    public void recibir(String proveedor, byte[] cuerpo, String firma) {
        String prov = proveedor.toLowerCase(Locale.ROOT);
        verificarFirma(prov, cuerpo, firma);

        WebhookPago w;
        try {
            w = om.readValue(cuerpo, WebhookPago.class);
        } catch (IOException e) {
            throw BusinessException.badRequest("SOLICITUD_INVALIDA", "Cuerpo del webhook inválido.");
        }
        if (w.codigoPago() == null || w.codigoPago().isBlank() || w.numeroOperacion() == null
                || w.numeroOperacion().isBlank() || w.monto() == null || w.monto().signum() <= 0) {
            throw BusinessException.badRequest("SOLICITUD_INVALIDA", "Faltan datos obligatorios en el webhook.");
        }

        EventoPago ev = new EventoPago();
        ev.setProveedor(prov);
        ev.setIdempotencia(w.numeroOperacion().trim());
        ev.setPayload(new String(cuerpo, StandardCharsets.UTF_8));
        ev.setEstado(EstadoEventoPago.PENDIENTE);
        ev.setFechaRecepcion(clock.instant());
        try {
            ev = eventos.saveAndFlush(ev);
        } catch (DataIntegrityViolationException duplicado) {
            log.info("Webhook {} repetido (op. {}): se ignora, ya fue recibido", prov, w.numeroOperacion());
            return; // idempotente: el banco recibe 200 y no se duplica nada
        }
        procesarSeguro(ev.getId());
    }

    /** Procesa un evento; ante cualquier error lo deja en ERROR para el reintento automático. */
    public void procesarSeguro(Long eventoId) {
        try {
            procesador.procesar(eventoId);
        } catch (Exception ex) {
            log.error("Error procesando evento de pago {}: {}", eventoId, ex.toString());
            procesador.marcarError(eventoId, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    /** Reintenta los eventos pendientes o con error (lo invoca el scheduler). */
    public int reintentarPendientes() {
        List<EventoPago> lista = eventos.findTop50ByEstadoInAndIntentosLessThanOrderByIdAsc(
                EnumSet.of(EstadoEventoPago.PENDIENTE, EstadoEventoPago.ERROR), props.getPagos().getMaxReintentos());
        lista.forEach(e -> procesarSeguro(e.getId()));
        return lista.size();
    }

    private void verificarFirma(String proveedor, byte[] cuerpo, String firma) {
        String secreto = props.getIntegraciones().getWebhookSecrets().get(proveedor);
        if (secreto == null || secreto.isBlank()) {
            throw BusinessException.notFound("Proveedor de pagos no configurado.");
        }
        if (firma == null || firma.isBlank()) {
            throw BusinessException.unauthorized("FIRMA_INVALIDA", "Firma del webhook inválida.");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] esperado = mac.doFinal(cuerpo);
            String hex = firma.trim();
            if (hex.regionMatches(true, 0, "sha256=", 0, 7)) {
                hex = hex.substring(7);
            }
            byte[] recibido = HexFormat.of().parseHex(hex);
            if (!MessageDigest.isEqual(esperado, recibido)) {
                throw BusinessException.unauthorized("FIRMA_INVALIDA", "Firma del webhook inválida.");
            }
        } catch (IllegalArgumentException e) {
            throw BusinessException.unauthorized("FIRMA_INVALIDA", "Firma del webhook inválida.");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo verificar la firma", e);
        }
    }
}
