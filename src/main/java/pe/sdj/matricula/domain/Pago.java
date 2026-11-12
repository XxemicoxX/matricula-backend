package pe.sdj.matricula.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_codigo_pago")
    private CodigoPago codigoPago;

    /** Usuario que registró el pago (solo pagos manuales). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(nullable = false, length = 50)
    private String proveedor;

    @Column(name = "numero_operacion", nullable = false, length = 100)
    private String numeroOperacion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_pago", nullable = false)
    private Instant fechaPago;

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", nullable = false, length = 20)
    private TipoPago tipoPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPago estado;

    @Column(name = "entidad_financiera", length = 100)
    private String entidadFinanciera;

    @Column(name = "referencia_comprobante", length = 150)
    private String referenciaComprobante;

    @Column(name = "fecha_registro", nullable = false)
    private Instant fechaRegistro;
}
