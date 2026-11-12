package pe.sdj.matricula.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.sdj.matricula.repository.SesionRevocadaRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Autentica con "Authorization: Bearer <jwt>". Si el token es válido y la sesión no fue cerrada,
 * deja al usuario en el contexto y entrega un token renovado en la cabecera X-Renewed-Token.
 * Si no hay token válido, la petición continúa sin autenticar y Spring Security responde 401.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_RENOVADO = "X-Renewed-Token";

    private final JwtService jwt;
    private final SesionRevocadaRepository revocadas;

    public JwtAuthFilter(JwtService jwt, SesionRevocadaRepository revocadas) {
        this.jwt = jwt;
        this.revocadas = revocadas;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims c = jwt.parsear(header.substring(7).trim());
                if (!revocadas.existsById(c.getId())) {
                    autenticar(c, response);
                }
            } catch (JwtException | IllegalArgumentException | ClassCastException e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private void autenticar(Claims c, HttpServletResponse response) {
        Object aid = c.get("aid");
        Long apoderadoId = aid instanceof Number n ? n.longValue() : null;
        Instant authTime = Instant.ofEpochSecond(((Number) c.get("auth")).longValue());
        UsuarioActual u = new UsuarioActual(Long.valueOf(c.getSubject()), c.get("rol", String.class),
                apoderadoId, c.get("nom", String.class), c.getId(), authTime);

        List<String> permisos = new ArrayList<>();
        if (c.get("perm") instanceof List<?> lista) {
            lista.forEach(p -> permisos.add(String.valueOf(p)));
        }
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + u.rol()));
        permisos.forEach(p -> autoridades.add(new SimpleGrantedAuthority(p)));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, autoridades));

        if (jwt.puedeRenovar(authTime)) {
            response.setHeader(HEADER_RENOVADO, jwt.emitir(u, permisos, authTime));
        }
    }
}
