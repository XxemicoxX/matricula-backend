package pe.sdj.matricula.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Cambio de datos fijos (CU-5/CU-19) o caso excepcional (CU-26/CU-18): mismo flujo y misma tabla. */
@Entity
@Table(name = "solicitud_modificacion")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudModificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante")
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_solicitante")
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_revisor")
    private Usuario revisor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_solicitud", nullable = false, length = 50)
    private TipoSolicitud tipoSolicitud;

    /** Solo casos excepcionales (MotivoExcepcion). */
    @Column(length = 50)
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** JSON {campo: valorNuevo} para cambios de datos fijos. */
    @Column(name = "datos_solicitados", columnDefinition = "TEXT")
    private String datosSolicitados;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(name = "fecha_solicitud", nullable = false)
    private Instant fechaSolicitud;

    @Column(name = "fecha_respuesta")
    private Instant fechaRespuesta;

    @Column(columnDefinition = "TEXT")
    private String observacion;
}
