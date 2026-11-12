package pe.sdj.matricula.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_matricula")
    private Matricula matricula;

    /** Destinatario apoderado (null si va dirigida a un rol del personal). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_apoderado")
    private Apoderado apoderado;

    /** Rol del personal destinatario (null si va a un apoderado). */
    @Column(name = "rol_destino", length = 50)
    private String rolDestino;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(nullable = false, length = 20)
    private String canal;

    @Column(length = 150)
    private String destinatario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "fecha_envio", nullable = false)
    private Instant fechaEnvio;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(nullable = false)
    private boolean leida;
}
