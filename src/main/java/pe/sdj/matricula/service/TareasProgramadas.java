package pe.sdj.matricula.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Procesos automáticos: liberar reservas vencidas, reintentar pagos pendientes y depurar sesiones. */
@Component
@RequiredArgsConstructor
@Slf4j
public class TareasProgramadas {

    private final MatriculaService matriculas;
    private final PagoWebhookService webhook;
    private final AuthService auth;

    @Scheduled(fixedDelayString = "${app.reservas.barrido-ms:60000}", initialDelayString = "${app.reservas.barrido-ms:60000}")
    public void liberarReservasVencidas() {
        try {
            int n = matriculas.expirarVencidas();
            if (n > 0) log.info("Reservas vencidas liberadas: {}", n);
        } catch (Exception e) {
            log.error("Fallo al liberar reservas vencidas", e);
        }
    }

    @Scheduled(fixedDelayString = "${app.pagos.reintento-ms:60000}", initialDelayString = "${app.pagos.reintento-ms:60000}")
    public void reintentarPagos() {
        try {
            int n = webhook.reintentarPendientes();
            if (n > 0) log.info("Eventos de pago reintentados: {}", n);
        } catch (Exception e) {
            log.error("Fallo al reintentar pagos", e);
        }
    }

    @Scheduled(cron = "0 15 * * * *")
    public void depurarSesionesRevocadas() {
        try {
            auth.purgarRevocadas();
        } catch (Exception e) {
            log.error("Fallo al depurar sesiones revocadas", e);
        }
    }
}
