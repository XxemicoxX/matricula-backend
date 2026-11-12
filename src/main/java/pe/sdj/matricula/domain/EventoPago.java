package pe.sdj.matricula.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Confirmación recibida del banco, guardada antes de procesarse (no se pierde ni se duplica). */
@Entity
@Table(name = "evento_pago")
@Getter
@Setter
@NoArgsConstructor
public class EventoPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long id;

    @Column(nullable = false, length = 50)
    private String proveedor;

    @Column(nullable = false, length = 100)
    private String idempotencia;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEventoPago estado = EstadoEventoPago.PENDIENTE;

    @Column(nullable = false)
    private int intentos;

    @Column(length = 500)
    private String detalle;

    @Column(name = "fecha_recepcion", nullable = false)
    private Instant fechaRecepcion;

    @Column(name = "fecha_procesamiento")
    private Instant fechaProcesamiento;
}
