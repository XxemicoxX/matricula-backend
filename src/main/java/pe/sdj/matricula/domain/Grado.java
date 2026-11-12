package pe.sdj.matricula.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "grado")
@Getter
@Setter
@NoArgsConstructor
public class Grado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_grado")
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String nivel;

    @Column(name = "monto_matricula", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoMatricula;
}
