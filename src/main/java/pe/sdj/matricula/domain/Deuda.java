package pe.sdj.matricula.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "deuda")
@Getter
@Setter
@NoArgsConstructor
public class Deuda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_deuda")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante")
    private Estudiante estudiante;

    @Column(nullable = false, length = 20)
    private String periodo;

    @Column(nullable = false, length = 100)
    private String concepto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoDeuda estado;

    @Column(name = "fecha_consulta", nullable = false)
    private Instant fechaConsulta;
}
