package pe.sdj.matricula.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "estudiante")
@Getter
@Setter
@NoArgsConstructor
public class Estudiante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estudiante")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_apoderado")
    private Apoderado apoderado;

    /** Grado al que postula en el periodo vigente (precargado desde el historial). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_grado_destino")
    private Grado gradoDestino;

    @Column(nullable = false, unique = true, length = 20)
    private String dni;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(length = 20)
    private String genero;

    @Column(length = 150)
    private String correo;

    @Column(length = 20)
    private String telefono;

    @Column(length = 200)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CondicionEstudiante condicion = CondicionEstudiante.REGULAR;

    /** RN-10: true cuando Secretaría aprobó un caso excepcional. */
    @Column(name = "excepcion_aprobada", nullable = false)
    private boolean excepcionAprobada;

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }
}
