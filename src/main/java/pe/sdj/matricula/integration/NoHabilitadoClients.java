package pe.sdj.matricula.integration;

import pe.sdj.matricula.common.BusinessException;

import java.time.LocalDate;
import java.util.List;

/**
 * Clientes de las integraciones reales mientras no se implementen: responden 503
 * ("integración no habilitada", documento 11.5) en vez de fallar de forma confusa.
 */
public final class NoHabilitadoClients {

    private NoHabilitadoClients() {
    }

    public static class Cubicol implements CubicolClient {
        @Override
        public String urlAutorizacion(String state, String pistaUsuario) {
            throw noDisponible();
        }

        @Override
        public CubicolIdentidad canjearCodigo(String code) {
            throw noDisponible();
        }

        @Override
        public List<DeudaExterna> consultarDeudas(String dniEstudiante, String periodo) {
            throw noDisponible();
        }

        private BusinessException noDisponible() {
            return BusinessException.unavailable("CUBICOL_NO_DISPONIBLE",
                    "El servicio externo (CUBICOL) no está disponible en este momento. Inténtalo más tarde.");
        }
    }

    public static class Minedu implements MineduClient {
        @Override
        public ResultadoValidacion validar(String dni, String nombres, String apellidos, LocalDate fechaNacimiento) {
            throw BusinessException.unavailable("MINEDU_NO_DISPONIBLE",
                    "El servicio externo (MINEDU) no está disponible en este momento. Inténtalo más tarde.");
        }
    }
}
