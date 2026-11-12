package pe.sdj.matricula.integration;

import org.springframework.web.util.UriComponentsBuilder;
import pe.sdj.matricula.common.BusinessException;
import pe.sdj.matricula.domain.Roles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * CUBICOL simulado para desarrollo y pruebas. NUNCA se habilita en producción (ver IntegracionConfig).
 * El "código" del callback es directamente el id SSO del usuario de prueba.
 */
public class StubCubicolClient implements CubicolClient {

    private static final String PERIODO_ANTERIOR_DEMO = "2026";

    private static final Map<String, CubicolIdentidad> USUARIOS = Map.of(
            "apoderado1", new CubicolIdentidad("apoderado1", "43876532", "Lucía", "Mendoza Rojas", "lucia.mendoza@example.com", Roles.APODERADO),
            "apoderado2", new CubicolIdentidad("apoderado2", "40123456", "Carlos", "Quispe Huamán", "carlos.quispe@example.com", Roles.APODERADO),
            "apoderado3", new CubicolIdentidad("apoderado3", "41234567", "Rosa", "Flores Paredes", "rosa.flores@example.com", Roles.APODERADO),
            "apoderado4", new CubicolIdentidad("apoderado4", "41000004", "Pedro", "Salas Vega", "pedro.salas@example.com", Roles.APODERADO),
            "subdirector", new CubicolIdentidad("subdirector", "10000001", "Ana", "Torres Vega", "subdireccion@example.com", Roles.SUBDIRECTOR),
            "secretaria", new CubicolIdentidad("secretaria", "10000002", "María", "Campos Díaz", "secretaria@example.com", Roles.SECRETARIA),
            "tesoreria", new CubicolIdentidad("tesoreria", "10000003", "Jorge", "Ramos Cruz", "tesoreria@example.com", Roles.TESORERIA));

    /** Estudiantes de prueba con deuda del año anterior (DNI -> deudas). */
    private static final Map<String, List<DeudaExterna>> DEUDAS = Map.of(
            "76543299", List.of(new DeudaExterna(PERIODO_ANTERIOR_DEMO, "Pensión octubre 2026", new BigDecimal("350.00"))));

    @Override
    public String urlAutorizacion(String state, String pistaUsuario) {
        String usuario = pistaUsuario == null || pistaUsuario.isBlank() ? "apoderado1" : pistaUsuario;
        return UriComponentsBuilder.fromPath("/api/v1/auth/sso/callback")
                .queryParam("code", usuario).queryParam("state", state)
                .build().encode().toUriString();
    }

    @Override
    public CubicolIdentidad canjearCodigo(String code) {
        CubicolIdentidad id = code == null ? null : USUARIOS.get(code);
        if (id == null) {
            throw BusinessException.unauthorized("SSO_INVALIDO",
                    "No se pudo validar tu identidad con CUBICOL. Inténtalo nuevamente.");
        }
        return id;
    }

    @Override
    public List<DeudaExterna> consultarDeudas(String dniEstudiante, String periodo) {
        return DEUDAS.getOrDefault(dniEstudiante, List.of()).stream()
                .filter(d -> d.periodo().equals(periodo)).toList();
    }
}
