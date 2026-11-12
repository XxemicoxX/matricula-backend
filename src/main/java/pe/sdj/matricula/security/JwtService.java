package pe.sdj.matricula.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import pe.sdj.matricula.config.AppProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Emite y valida el token de sesión. El token vive 15 minutos y se renueva (deslizante) con cada
 * petición, de modo que la sesión se cierra tras 15 minutos de inactividad; la sesión completa
 * está acotada por app.jwt.sesion-maxima-horas.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Clock clock;
    private final Duration inactividad;
    private final Duration sesionMaxima;

    public JwtService(AppProperties props, Clock clock) {
        String secret = props.getJwt().getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret (JWT_SECRET) debe tener al menos 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
        this.inactividad = Duration.ofMinutes(props.getJwt().getInactividadMinutos());
        this.sesionMaxima = Duration.ofHours(props.getJwt().getSesionMaximaHoras());
    }

    public String emitir(UsuarioActual u, Collection<String> permisos, Instant authTime) {
        Instant ahora = clock.instant();
        JwtBuilder b = Jwts.builder()
                .id(u.sid())
                .subject(String.valueOf(u.id()))
                .claim("rol", u.rol())
                .claim("nom", u.nombre())
                .claim("perm", List.copyOf(permisos))
                .claim("auth", authTime.getEpochSecond())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(inactividad)));
        if (u.apoderadoId() != null) {
            b.claim("aid", u.apoderadoId());
        }
        return b.signWith(key).compact();
    }

    /** Lanza JwtException si el token es inválido, está alterado o expiró. */
    public Claims parsear(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean puedeRenovar(Instant authTime) {
        return clock.instant().isBefore(authTime.plus(sesionMaxima));
    }

    public Instant finMaximoDeSesion(Instant authTime) {
        return authTime.plus(sesionMaxima);
    }

    public Duration inactividad() {
        return inactividad;
    }
}
