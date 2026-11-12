package pe.sdj.matricula.integration;

import java.time.LocalDate;

/** Puerto hacia el servicio de interoperabilidad del MINEDU (RN-06). */
public interface MineduClient {

    /** Lanza 503/504 si el servicio no está disponible. */
    ResultadoValidacion validar(String dni, String nombres, String apellidos, LocalDate fechaNacimiento);

    record ResultadoValidacion(boolean valido, String observacion) {
    }
}
