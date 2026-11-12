package pe.sdj.matricula.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Notificador por defecto: registra el envío en el log (sin el contenido del mensaje).
 * Reemplazar por una implementación SMTP/SMS definiendo otro bean NotificadorExterno.
 */
@Component
@Slf4j
public class LogNotificador implements NotificadorExterno {

    @Override
    public boolean enviar(String canal, String destinatario, String asunto, String mensaje) {
        log.info("Notificación [{}] a {} - {}", canal, destinatario, asunto);
        return true;
    }
}
