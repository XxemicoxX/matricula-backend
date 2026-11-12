package pe.sdj.matricula.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.sdj.matricula.common.ApiResponse;
import pe.sdj.matricula.dto.SeccionDtos.*;
import pe.sdj.matricula.security.UsuarioActual;
import pe.sdj.matricula.service.SeccionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/secciones")
@RequiredArgsConstructor
@Tag(name = "Secciones")
public class SeccionController {

    private final SeccionService service;

    @GetMapping
    @PreAuthorize("hasAuthority('CONSULTAR_SECCIONES')")
    public ApiResponse<List<SeccionDto>> listar(@AuthenticationPrincipal UsuarioActual u,
                                                @RequestParam(required = false) Long estudianteId,
                                                @RequestParam(required = false) Long gradoId) {
        return ApiResponse.ok(service.listar(u, estudianteId, gradoId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('GESTIONAR_SECCIONES')")
    public ApiResponse<SeccionDto> crear(@Valid @RequestBody CrearSeccionRequest req) {
        return ApiResponse.ok(service.crear(req));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('GESTIONAR_SECCIONES')")
    public ApiResponse<SeccionDto> modificar(@PathVariable Long id, @Valid @RequestBody ModificarSeccionRequest req) {
        return ApiResponse.ok(service.modificar(id, req));
    }
}
