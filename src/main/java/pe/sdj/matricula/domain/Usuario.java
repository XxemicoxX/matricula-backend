package pe.sdj.matricula.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_rol")
    private Rol rol;

    /** Solo para usuarios con rol APODERADO: vínculo con el apoderado precargado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_apoderado")
    private Apoderado apoderado;

    @Column(name = "identificador_sso", nullable = false, unique = true, length = 100)
    private String identificadorSso;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(length = 150)
    private String correo;

    @Column(nullable = false, length = 20)
    private String estado = "ACTIVO";

    public boolean activo() {
        return "ACTIVO".equals(estado);
    }

    public String nombreCompleto() {
        return nombres + " " + apellidos;
    }
}
