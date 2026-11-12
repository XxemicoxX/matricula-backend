package pe.sdj.matricula.integration;

import java.math.BigDecimal;
import java.util.List;

/**
 * Puerto hacia CUBICOL: proveedor de identidad (SSO) y fuente del estado financiero (deudas).
 * El contrato real de CUBICOL aún no está definido; la implementación productiva se enchufa aquí
 * sin tocar el resto del sistema (ver IntegracionConfig).
 */
public interface CubicolClient {

    /** URL a la que se redirige al usuario para iniciar sesión en CUBICOL. */
    String urlAutorizacion(String state, String pistaUsuario);

    /** Canjea el código devuelto por CUBICOL por la identidad del usuario. Lanza 401 si es inválido. */
    CubicolIdentidad canjearCodigo(String code);

    /** Deudas del estudiante en el periodo indicado. Lanza 503/504 si CUBICOL no responde. */
    List<DeudaExterna> consultarDeudas(String dniEstudiante, String periodo);

    record CubicolIdentidad(String ssoId, String dni, String nombres, String apellidos, String correo, String rol) {
    }

    record DeudaExterna(String periodo, String concepto, BigDecimal monto) {
    }
}
