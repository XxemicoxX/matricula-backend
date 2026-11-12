package pe.sdj.matricula.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import pe.sdj.matricula.integration.*;

/** Selecciona la implementación de cada integración según app.integraciones.*.modo. */
@Configuration
public class IntegracionConfig {

    @Bean
    public CubicolClient cubicolClient(AppProperties props, Environment env) {
        if ("stub".equalsIgnoreCase(props.getIntegraciones().getCubicol().getModo())) {
            prohibirStubEnProduccion(env, "CUBICOL");
            return new StubCubicolClient();
        }
        return new NoHabilitadoClients.Cubicol();
    }

    @Bean
    public MineduClient mineduClient(AppProperties props, Environment env) {
        if ("stub".equalsIgnoreCase(props.getIntegraciones().getMinedu().getModo())) {
            prohibirStubEnProduccion(env, "MINEDU");
            return new StubMineduClient();
        }
        return new NoHabilitadoClients.Minedu();
    }

    private void prohibirStubEnProduccion(Environment env, String nombre) {
        if (env.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("La integración simulada de " + nombre + " no puede usarse en el perfil prod");
        }
    }
}
