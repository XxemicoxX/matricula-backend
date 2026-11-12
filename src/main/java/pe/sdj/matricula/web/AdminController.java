package pe.sdj.matricula.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.sdj.matricula.common.ApiResponse;
import pe.sdj.matricula.domain.EstadoMatricula;
import pe.sdj.matricula.dto.AdminDtos.*;
import pe.sdj.matricula.security.UsuarioActual;
import pe.sdj.matricula.service.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Dashboard, reportes, auditoría y notificaciones")
public class AdminController {

    private final DashboardService dashboard;
    private final ReporteService reportes;
    private final AuditoriaService auditoria;
    private final NotificacionService notificaciones;

    @GetMapping("/dashboard/aforo")
    @PreAuthorize("hasAuthority('CONSULTAR_DASHBOARD')")
    public ApiResponse<DashboardDto> aforo() {
        return ApiResponse.ok(dashboard.aforo());
    }

    @GetMapping("/reportes/matriculas")
    @PreAuthorize("hasAuthority('GENERAR_REPORTES')")
    public ApiResponse<List<FilaReporteDto>> reporte(@RequestParam(required = false) String periodo,
                                                     @RequestParam(required = false) Long gradoId,
                                                     @RequestParam(required = false) Long seccionId,
                                                     @RequestParam(required = false) EstadoMatricula estado,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                     Pageable pageable) {
        return ApiResponse.page(reportes.consultar(periodo, gradoId, seccionId, estado, desde, hasta, pageable));
    }

    @GetMapping("/reportes/matriculas/exportacion")
    @PreAuthorize("hasAuthority('GENERAR_REPORTES')")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) String periodo,
                                           @RequestParam(required = false) Long gradoId,
                                           @RequestParam(required = false) Long seccionId,
                                           @RequestParam(required = false) EstadoMatricula estado,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        byte[] xlsx = reportes.exportar(periodo, gradoId, seccionId, estado, desde, hasta);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("reporte-matriculas.xlsx").build().toString())
                .body(xlsx);
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasAuthority('CONSULTAR_AUDITORIA')")
    public ApiResponse<List<AuditoriaDto>> auditoria(@RequestParam(required = false) Long usuarioId,
                                                     @RequestParam(required = false) String accion,
                                                     @RequestParam(required = false) Instant desde,
                                                     @RequestParam(required = false) Instant hasta, Pageable pageable) {
        return ApiResponse.page(auditoria.consultar(usuarioId, accion, desde, hasta, pageable));
    }

    @GetMapping("/notificaciones")
    public ApiResponse<List<NotificacionDto>> notificaciones(@AuthenticationPrincipal UsuarioActual u, Pageable pageable) {
        return ApiResponse.page(notificaciones.misNotificaciones(u, pageable));
    }

    @PostMapping("/notificaciones/{id}/leida")
    public ApiResponse<Void> leida(@AuthenticationPrincipal UsuarioActual u, @PathVariable Long id) {
        notificaciones.marcarLeida(id, u);
        return ApiResponse.ok(null);
    }
}
