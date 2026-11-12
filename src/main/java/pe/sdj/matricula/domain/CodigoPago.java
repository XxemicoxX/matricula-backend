package pe.sdj.matricula.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "codigo_pago")
@Getter
@Setter
@NoArgsConstructor
public class CodigoPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_codigo_pago")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_matricula", unique = true)
    private Matricula matricula;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_generacion", nullable = false)
    private Instant fechaGeneracion;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoCodigoPago estado;
}
