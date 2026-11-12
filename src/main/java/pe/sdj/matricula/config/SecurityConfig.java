package pe.sdj.matricula.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pe.sdj.matricula.common.ApiResponse;
import pe.sdj.matricula.common.GlobalExceptionHandler;
import pe.sdj.matricula.repository.SesionRevocadaRepository;
import pe.sdj.matricula.security.JwtAuthFilter;
import pe.sdj.matricula.security.JwtService;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AppProperties props;
    private final JwtService jwtService;
    private final SesionRevocadaRepository revocadas;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API sin cookies de sesión: el token viaja en Authorization
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/sso/**").permitAll()
                        // El webhook del banco se autentica con firma HMAC, no con JWT
                        .requestMatchers("/api/v1/webhooks/**").permitAll()
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> escribir(res, HttpStatus.UNAUTHORIZED,
                                "NO_AUTENTICADO", GlobalExceptionHandler.MSG_401))
                        .accessDeniedHandler((req, res, ex) -> escribir(res, HttpStatus.FORBIDDEN,
                                "SIN_PERMISOS", GlobalExceptionHandler.MSG_403)))
                .addFilterBefore(new JwtAuthFilter(jwtService, revocadas), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(props.getCors().getAllowedOrigins());
        cfg.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
        cfg.setExposedHeaders(List.of(JwtAuthFilter.HEADER_RENOVADO, "X-Request-Id", "Content-Disposition"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }

    private void escribir(HttpServletResponse res, HttpStatus status, String codigo, String mensaje) throws IOException {
        res.setStatus(status.value());
        res.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(res.getOutputStream(), ApiResponse.error(codigo, mensaje, null));
    }
}
