package pe.sdj.matricula.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.sdj.matricula.common.ApiResponse;
import pe.sdj.matricula.dto.MatriculaDtos.*;
import pe.sdj.matricula.security.UsuarioActual;
import pe.sdj.matricula.service.MatriculaService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matriculas")
@RequiredArgsConstructor
@Tag(name = "Matrículas")
public class MatriculaController {

    private final MatriculaService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('GENERAR_CODIGO_PAGO')")
    public ApiResponse<MatriculaDto> reservar(@AuthenticationPrincipal UsuarioActual u, @Valid @RequestBody ReservaRequest req) {
        return ApiResponse.ok(service.reservar(u, req));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONSULTAR_ESTUDIANTE')")
    public ApiResponse<List<MatriculaDto>> listar(@AuthenticationPrincipal UsuarioActual u, @RequestParam Long estudianteId) {
        return ApiResponse.ok(service.listarPorEstudiante(estudianteId, u));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONSULTAR_ESTUDIANTE')")
    public ApiResponse<MatriculaDto> obtener(@AuthenticationPrincipal UsuarioActual u, @PathVariable Long id) {
        return ApiResponse.ok(service.obtener(id, u));
    }

    @GetMapping("/{id}/constancia")
    @PreAuthorize("hasAuthority('CONSULTAR_ESTUDIANTE')")
    public ResponseEntity<byte[]> constancia(@AuthenticationPrincipal UsuarioActual u, @PathVariable Long id) {
        MatriculaService.Constancia c = service.constancia(id, u);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(c.nombreArchivo()).build().toString())
                .body(c.contenido());
    }
}
