package pe.sdj.matricula.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String periodoActual = "2027";
    private String zonaHoraria = "America/Lima";
    private String frontendUrl = "http://localhost:5173";
    private Cors cors = new Cors();
    private Jwt jwt = new Jwt();
    private Matricula matricula = new Matricula();
    private Integraciones integraciones = new Integraciones();
    private Storage storage = new Storage();
    private Pagos pagos = new Pagos();
    private Reservas reservas = new Reservas();

    /** Periodo del que se exigen deudas (RN-01): el inmediatamente anterior al vigente. */
    public String periodoAnterior() {
        return String.valueOf(Integer.parseInt(periodoActual) - 1);
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173");
    }

    @Data
    public static class Jwt {
        private String secret;
        private int inactividadMinutos = 15;
        private int sesionMaximaHoras = 8;
    }

    @Data
    public static class Matricula {
        private int reservaHoras = 48;
        private BigDecimal montoDefecto = new BigDecimal("650.00");
    }

    @Data
    public static class Integraciones {
        private Modo cubicol = new Modo();
        private Modo minedu = new Modo();
        /** proveedor (minúsculas) -> secreto HMAC-SHA256 del webhook. */
        private Map<String, String> webhookSecrets = new HashMap<>();
    }

    @Data
    public static class Modo {
        /** stub (simulado, solo fuera de prod) | real. */
        private String modo = "real";
    }

    @Data
    public static class Storage {
        private String ruta = "./storage";
    }

    @Data
    public static class Pagos {
        private int maxReintentos = 5;
        private long reintentoMs = 60000;
    }

    @Data
    public static class Reservas {
        private long barridoMs = 60000;
    }

}
