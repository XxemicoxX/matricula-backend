package pe.sdj.matricula.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sdj.matricula.domain.ValidacionMinedu;

import java.util.Optional;

public interface ValidacionMineduRepository extends JpaRepository<ValidacionMinedu, Long> {
    Optional<ValidacionMinedu> findFirstByEstudianteIdAndPeriodoAcademicoOrderByIdDesc(Long estudianteId, String periodo);
}
