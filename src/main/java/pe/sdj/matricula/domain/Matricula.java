package pe.sdj.matricula.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "matricula")
@Getter
@Setter
@NoArgsConstructor
public class Matricula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_matricula")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante")
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_seccion")
    private Seccion seccion;

    @Column(name = "periodo_academico", nullable = false, length = 20)
    private String periodoAcademico;

    /** Se completa solo al confirmarse el pago (RN-07). */
    @Column(name = "fecha_matricula")
    private Instant fechaMatricula;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoMatricula estado;

    @Column(name = "fecha_reserva", nullable = false)
    private Instant fechaReserva;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;
}
