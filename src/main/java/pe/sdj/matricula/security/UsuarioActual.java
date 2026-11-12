package pe.sdj.matricula.security;

import pe.sdj.matricula.domain.Roles;

import java.time.Instant;

/**
 * Usuario autenticado, reconstruido desde el JWT en cada petición.
 *
 * @param sid      identificador de sesión (jti), usado para cerrar sesión
 * @param authTime momento del inicio de sesión SSO (limita la duración máxima de la sesión)
 */
public record UsuarioActual(Long id, String rol, Long apoderadoId, String nombre, String sid, Instant authTime) {

    public boolean esApoderado() {
        return Roles.APODERADO.equals(rol);
    }
}
