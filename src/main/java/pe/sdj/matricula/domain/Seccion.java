package pe.sdj.matricula.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seccion")
@Getter
@Setter
@NoArgsConstructor
public class Seccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seccion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_grado")
    private Grado grado;

    @Column(nullable = false, length = 10)
    private String nombre;

    @Column(length = 50)
    private String aula;

    @Column(name = "docente_responsable", length = 150)
    private String docenteResponsable;

    @Column(name = "aforo_maximo", nullable = false)
    private int aforoMaximo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSeccion estado = EstadoSeccion.ABIERTA;
}
