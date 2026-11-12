package pe.sdj.matricula.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sdj.matricula.domain.CodigoPago;

import java.util.Optional;

public interface CodigoPagoRepository extends JpaRepository<CodigoPago, Long> {
    Optional<CodigoPago> findByCodigo(String codigo);

    Optional<CodigoPago> findByMatriculaId(Long matriculaId);

    boolean existsByCodigo(String codigo);
}
