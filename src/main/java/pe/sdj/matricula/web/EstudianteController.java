package pe.sdj.matricula.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.sdj.matricula.common.ApiResponse;
import pe.sdj.matricula.dto.EstudianteDtos.*;
import pe.sdj.matricula.security.UsuarioActual;
import pe.sdj.matricula.service.EstudianteService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/estudiantes")
@RequiredArgsConstructor
@Tag(name = "Estudiantes")
public class EstudianteController {

    private final EstudianteService service;

    @GetMapping
    @PreAuthorize("hasAuthority('CONSULTAR_ESTUDIANTE')")
    public ApiResponse<List<EstudianteDto>> listar(@AuthenticationPrincipal UsuarioActual u,
                                                   @RequestParam(required = false) String q, Pageable pageable) {
        return ApiResponse.page(service.listar(u, q, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSULTAR_ESTUDIANTE')")
    public ApiResponse<EstudianteDto> obtener(@AuthenticationPrincipal UsuarioActual u, @PathVariable Long id) {
        return ApiResponse.ok(service.obtener(id, u));
    }

    @PatchMapping("/{id}/contacto")
    @PreAuthorize("hasAuthority('ACTUALIZAR_CONTACTO')")
    public ApiResponse<EstudianteDto> contacto(@AuthenticationPrincipal UsuarioActual u, @PathVariable Long id,
                                               @Valid @RequestBody ContactoRequest req) {
        return ApiResponse.ok(service.actualizarContacto(id, req, u));
    }

    @GetMapping("/{id}/validaciones/deudas")
    @PreAuthorize("hasAuthority('CONSULTAR_DEUDAS')")
    public ApiResponse<ResultadoDeudasDto> deudas(@AuthenticationPrincipal UsuarioActual u, @PathVariable Long id) {
        return ApiResponse.ok(service.validarDeudas(id, u));
    }

    @GetMapping("/{id}/validaciones/minedu")
    @PreAuthorize("hasAuthority('GENERAR_CODIGO_PAGO')")
    public ApiResponse<ValidacionMineduDto> minedu(@AuthenticationPrincipal UsuarioActual u, @PathVariable Long id) {
        return ApiResponse.ok(service.validarMinedu(id, u));
    }
}
