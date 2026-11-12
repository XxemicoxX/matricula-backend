package pe.sdj.matricula.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /** Reloj inyectable (UTC) para poder probar expiraciones sin esperar 48 horas. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
