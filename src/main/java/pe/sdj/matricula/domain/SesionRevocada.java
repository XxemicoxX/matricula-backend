package pe.sdj.matricula.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "sesion_revocada")
@Getter
@Setter
@NoArgsConstructor
public class SesionRevocada {
    @Id
    @Column(length = 64)
    private String jti;

    @Column(nullable = false)
    private Instant expira;

    public SesionRevocada(String jti, Instant expira) {
        this.jti = jti;
        this.expira = expira;
    }
}
