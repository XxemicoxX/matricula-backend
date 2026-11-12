package pe.sdj.matricula.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.sdj.matricula.domain.EstadoMatricula;

import java.math.BigDecimal;
import java.time.Instant;

public final class PagoDtos {

    private PagoDtos() {
    }

    /** POST /pagos/manuales (documento 11.3). */
    public record PagoManualRequest(
            @NotNull Long matriculaId,
            @NotBlank @Size(max = 100) String entidadFinanciera,
            @NotBlank @Size(max = 100) String numeroOperacion,
            @NotNull @DecimalMin("0.01") BigDecimal monto,
            @NotNull Instant fechaPago,
            @Size(max = 150) String referenciaComprobante) {
    }

    /** Cuerpo que envía el banco al webhook. */
    public record WebhookPago(
            @NotBlank String codigoPago,
            @NotBlank @Size(max = 100) String numeroOperacion,
            @NotNull @DecimalMin("0.01") BigDecimal monto,
            Instant fechaPago,
            @Size(max = 50) String metodoPago,
            String estado) {
    }

    public record PagoResultadoDto(String resultado, EstadoMatricula estadoMatricula, Long pagoId) {
    }
}
