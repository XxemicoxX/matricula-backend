package pe.sdj.matricula.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "validacion_minedu")
@Getter
@Setter
@NoArgsConstructor
public class ValidacionMinedu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_validacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante")
    private Estudiante estudiante;

    @Column(name = "periodo_academico", nullable = false, length = 20)
    private String periodoAcademico;

    @Column(name = "fecha_validacion", nullable = false)
    private Instant fechaValidacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ResultadoMinedu resultado;

    @Column(columnDefinition = "TEXT")
    private String observacion;
}
