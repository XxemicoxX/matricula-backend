package pe.sdj.matricula.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.sdj.matricula.common.ApiResponse;
import pe.sdj.matricula.domain.Documento;
import pe.sdj.matricula.domain.EstadoSolicitud;
import pe.sdj.matricula.domain.TipoSolicitud;
import pe.sdj.matricula.dto.SolicitudDtos.*;
import pe.sdj.matricula.security.UsuarioActual;
import pe.sdj.matricula.service.SolicitudService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Solicitudes y casos excepcionales")
public class SolicitudController {

    private final SolicitudService service;

    @PostMapping(value = "/solicitudes-modificacion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SOLICITAR_CAMBIO_DATOS')")
    public ApiResponse<SolicitudDto> crear(@AuthenticationPrincipal UsuarioActual u,
                                           @Valid @RequestPart("solicitud") CrearSolicitudRequest req,
                                           @RequestPart(value = "documentos", required = false) List<MultipartFile> documentos) {
        return ApiResponse.ok(service.crearCambioDatos(u, req, documentos));
    }

    @PostMapping(value = "/casos-excepcionales", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SOLICITAR_EXCEPCION')")
    public ApiResponse<SolicitudDto> crearCaso(@AuthenticationPrincipal UsuarioActual u,
                                               @Valid @RequestPart("solicitud") CrearCasoRequest req,
                                               @RequestPart(value = "documentos", required = false) List<MultipartFile> documentos) {
        return ApiResponse.ok(service.crearCasoExcepcional(u, req, documentos));
    }

    @GetMapping("/solicitudes-modificacion")
    @PreAuthorize("hasAnyAuthority('SOLICITAR_CAMBIO_DATOS','GESTIONAR_SOLICITUDES')")
    public ApiResponse<List<SolicitudDto>> listar(@AuthenticationPrincipal UsuarioActual u,
                                                  @RequestParam(required = false) EstadoSolicitud estado, Pageable p) {
        return ApiResponse.page(service.listar(u, TipoSolicitud.CAMBIO_DATOS, estado, p));
    }

    @GetMapping("/casos-excepcionales")
    @PreAuthorize("hasAnyAuthority('SOLICITAR_EXCEPCION','GESTIONAR_EXCEPCIONES')")
    public ApiResponse<List<SolicitudDto>> listarCasos(@AuthenticationPrincipal UsuarioActual u,
                                                       @RequestParam(required = false) EstadoSolicitud estado, Pageable p) {
        return ApiResponse.page(service.listar(u, TipoSolicitud.CASO_EXCEPCIONAL, estado, p));
    }

    @GetMapping({"/solicitudes-modificacion/{id}", "/casos-excepcionales/{id}"})
    @PreAuthorize("hasAnyAuthority('SOLICITAR_CAMBIO_DATOS','SOLICITAR_EXCEPCION','GESTIONAR_SOLICITUDES','GESTIONAR_EXCEPCIONES')")
    public ApiResponse<SolicitudDto> obtener(@AuthenticationPrincipal UsuarioActual u, @PathVariable Long id) {
        return ApiResponse.ok(service.obtener(id, u));
    }

    @PostMapping({"/solicitudes-modificacion/{id}/resolucion", "/casos-excepcionales/{id}/resolucion"})
    @PreAuthorize("hasAnyAuthority('GESTIONAR_SOLICITUDES','GESTIONAR_EXCEPCIONES')")
    public ApiResponse<SolicitudDto> resolver(@AuthenticationPrincipal UsuarioActual u, @PathVariable Long id,
                                              @Valid @RequestBody ResolverRequest req) {
        return ApiResponse.ok(service.resolver(id, req, u));
    }

    @GetMapping({"/solicitudes-modificacion/{id}/documentos/{docId}", "/casos-excepcionales/{id}/documentos/{docId}"})
    @PreAuthorize("hasAnyAuthority('SOLICITAR_CAMBIO_DATOS','SOLICITAR_EXCEPCION','GESTIONAR_SOLICITUDES','GESTIONAR_EXCEPCIONES')")
    public ResponseEntity<Resource> documento(@AuthenticationPrincipal UsuarioActual u, @PathVariable Long id,
                                              @PathVariable Long docId) {
        Resource r = service.documento(id, docId, u);
        Documento d = service.infoDocumento(id, docId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.getTipoDocumento()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(d.getNombreArchivo()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(r);
    }
}
