package pe.sdj.matricula.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.sdj.matricula.common.ApiResponse;
import pe.sdj.matricula.dto.AuthDtos.SesionDto;
import pe.sdj.matricula.dto.AuthDtos.TokenDto;
import pe.sdj.matricula.security.UsuarioActual;
import pe.sdj.matricula.service.AuthService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación (SSO CUBICOL)")
public class AuthController {

    private final AuthService auth;

    public record CallbackRequest(@NotBlank String code, String state) {
    }

    /** Devuelve la URL de CUBICOL a la que el frontend debe redirigir al usuario. */
    @GetMapping("/sso/inicio")
    public ApiResponse<Map<String, String>> inicio(@RequestParam(required = false) String hint) {
        String state = UUID.randomUUID().toString();
        return ApiResponse.ok(Map.of("urlAutorizacion", auth.urlInicio(state, hint), "state", state));
    }

    @PostMapping("/sso/callback")
    public ApiResponse<TokenDto> callback(@jakarta.validation.Valid @RequestBody CallbackRequest req) {
        return ApiResponse.ok(auth.iniciarSesion(req.code()));
    }

    @GetMapping("/sesion")
    public ApiResponse<SesionDto> sesion(@AuthenticationPrincipal UsuarioActual u, Authentication a) {
        List<String> permisos = a.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .filter(s -> !s.startsWith("ROLE_")).sorted().toList();
        return ApiResponse.ok(new SesionDto(u.id(), u.nombre(), u.rol(), permisos, u.apoderadoId()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal UsuarioActual u) {
        auth.cerrarSesion(u);
        return ApiResponse.ok(null);
    }
}
