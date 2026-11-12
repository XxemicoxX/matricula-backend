package pe.sdj.matricula.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "documento")
@Getter
@Setter
@NoArgsConstructor
public class Documento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud")
    private SolicitudModificacion solicitud;

    @Column(name = "nombre_archivo", nullable = false, length = 200)
    private String nombreArchivo;

    /** Ruta relativa dentro del almacenamiento; el nombre real lo genera el servidor. */
    @Column(name = "ruta_archivo", nullable = false, length = 300)
    private String rutaArchivo;

    @Column(name = "tipo_documento", nullable = false, length = 100)
    private String tipoDocumento;

    @Column(name = "fecha_subida", nullable = false)
    private Instant fechaSubida;
}
