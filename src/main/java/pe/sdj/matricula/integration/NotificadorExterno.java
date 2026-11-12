package pe.sdj.matricula.integration;

/** Puerto de salida hacia el servicio de correo/SMS. */
public interface NotificadorExterno {

    /** @return true si el mensaje fue entregado al proveedor. */
    boolean enviar(String canal, String destinatario, String asunto, String mensaje);
}
