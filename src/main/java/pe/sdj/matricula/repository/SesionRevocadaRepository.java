package pe.sdj.matricula.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.sdj.matricula.domain.SesionRevocada;

import java.time.Instant;

public interface SesionRevocadaRepository extends JpaRepository<SesionRevocada, String> {
    @Modifying
    @Query("delete from SesionRevocada s where s.expira < :ahora")
    int purgarExpiradas(@Param("ahora") Instant ahora);
}
