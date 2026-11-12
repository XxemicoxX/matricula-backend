package pe.sdj.matricula.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sdj.matricula.common.BusinessException;
import pe.sdj.matricula.domain.*;
import pe.sdj.matricula.dto.AuthDtos.SesionDto;
import pe.sdj.matricula.dto.AuthDtos.TokenDto;
import pe.sdj.matricula.integration.CubicolClient;
import pe.sdj.matricula.integration.CubicolClient.CubicolIdentidad;
import pe.sdj.matricula.repository.ApoderadoRepository;
import pe.sdj.matricula.repository.RolRepository;
import pe.sdj.matricula.repository.SesionRevocadaRepository;
import pe.sdj.matricula.repository.UsuarioRepository;
import pe.sdj.matricula.security.JwtService;
import pe.sdj.matricula.security.UsuarioActual;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** SSO con CUBICOL (RN-05): no se guardan contraseñas; el rol llega desde CUBICOL. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final CubicolClient cubicol;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final ApoderadoRepository apoderados;
    private final SesionRevocadaRepository revocadas;
    private final JwtService jwt;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public String urlInicio(String state, String pistaUsuario) {
        return cubicol.urlAutorizacion(state, pistaUsuario);
    }

    @Transactional
    public TokenDto iniciarSesion(String code) {
        CubicolIdentidad id = cubicol.canjearCodigo(code);

        Rol rol = roles.findByNombre(id.rol()).orElseThrow(() -> BusinessException.forbidden(
                "ROL_NO_AUTORIZADO", "Tu usuario no tiene un rol autorizado para usar este sistema."));

        Usuario u = usuarios.findByIdentificadorSso(id.ssoId()).orElseGet(Usuario::new);
        u.setIdentificadorSso(id.ssoId());
        u.setRol(rol);
        u.setNombres(id.nombres());
        u.setApellidos(id.apellidos());
        u.setCorreo(id.correo());
        if (!u.activo()) {
            throw BusinessException.forbidden("USUARIO_INACTIVO", "Tu cuenta se encuentra inactiva. Comunícate con la institución.");
        }

        if (Roles.APODERADO.equals(rol.getNombre())) {
            // Solo ingresan apoderados con datos precargados (RN-05/RN-06): se vinculan por DNI.
            Apoderado ap = apoderados.findByDni(id.dni()).orElseThrow(() -> BusinessException.forbidden(
                    "APODERADO_NO_REGISTRADO",
                    "No encontramos un apoderado registrado con tu identidad. Comunícate con la institución."));
            u.setApoderado(ap);
        } else {
            u.setApoderado(null);
        }
        u = usuarios.save(u);

        Instant ahora = clock.instant();
        List<String> permisos = permisos(rol);
        UsuarioActual actual = new UsuarioActual(u.getId(),
                rol.getNombre(), u.getApoderado() == null ? null : u.getApoderado().getId(),
                u.nombreCompleto(), UUID.randomUUID().toString(), ahora);
        String token = jwt.emitir(actual, permisos, ahora);

        auditoria.registrarComo(u.getId(), "INICIO_SESION", "Inicio de sesión SSO (" + rol.getNombre() + ")");
        return new TokenDto(token, "Bearer", jwt.inactividad().toSeconds(),
                new SesionDto(u.getId(), actual.nombre(), actual.rol(), permisos, actual.apoderadoId()));
    }

    /** Cierra la sesión actual (CU-25): el token deja de ser válido aunque no haya expirado. */
    @Transactional
    public void cerrarSesion(UsuarioActual u) {
        revocadas.save(new SesionRevocada(u.sid(), jwt.finMaximoDeSesion(u.authTime())));
        auditoria.registrarComo(u.id(), "CIERRE_SESION", "Cierre de sesión manual");
    }

    /** Elimina de la lista de revocadas las sesiones que ya no podrían usarse de todos modos. */
    @Transactional
    public int purgarRevocadas() {
        return revocadas.purgarExpiradas(clock.instant());
    }

    private List<String> permisos(Rol rol) {
        return rol.getPermisos().stream().map(Permiso::getNombre).sorted().toList();
    }
}
