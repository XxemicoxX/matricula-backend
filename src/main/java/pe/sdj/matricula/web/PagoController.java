package pe.sdj.matricula.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.sdj.matricula.common.ApiResponse;
import pe.sdj.matricula.dto.PagoDtos.*;
import org.springframework.web.multipart.MultipartFile;
import pe.sdj.matricula.security.UsuarioActual;
import pe.sdj.matricula.service.PagoService;
import pe.sdj.matricula.service.PagoWebhookService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Pagos")
public class PagoController {

    private final PagoService pagos;
    private final PagoWebhookService webhook;

    @PostMapping(value = "/pagos/manuales", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('REGISTRAR_PAGO_MANUAL')")
    public ApiResponse<PagoResultadoDto> manual(@AuthenticationPrincipal UsuarioActual u, @Valid @RequestBody PagoManualRequest req) {
        PagoService.ResultadoPago r = pagos.registrarManual(u, req);
        return ApiResponse.ok(new PagoResultadoDto(r.resultado().name(), r.estadoMatricula(), r.pagoId()));
    }

    /** Mismo registro con el voucher adjunto: partes "pago" (JSON) y "comprobante" (PDF, PNG o JPG). */
    @PostMapping(value = "/pagos/manuales", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('REGISTRAR_PAGO_MANUAL')")
    public ApiResponse<PagoResultadoDto> manualConComprobante(@AuthenticationPrincipal UsuarioActual u,
                                                              @Valid @RequestPart("pago") PagoManualRequest req,
                                                              @RequestPart(value = "comprobante", required = false) MultipartFile comprobante) {
        PagoService.ResultadoPago r = pagos.registrarManual(u, req, comprobante);
        return ApiResponse.ok(new PagoResultadoDto(r.resultado().name(), r.estadoMatricula(), r.pagoId()));
    }

    /** Público, autenticado por firma HMAC-SHA256 (cabecera X-Signature) sobre el cuerpo crudo. */
    @PostMapping("/webhooks/pagos/{proveedor}")
    public ApiResponse<Void> webhook(@PathVariable String proveedor, @RequestBody byte[] cuerpo,
                                     @RequestHeader(value = "X-Signature", required = false) String firma) {
        webhook.recibir(proveedor, cuerpo, firma);
        return ApiResponse.ok(null);
    }
}
