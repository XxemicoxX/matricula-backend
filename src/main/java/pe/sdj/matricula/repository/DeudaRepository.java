package pe.sdj.matricula.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sdj.matricula.domain.Deuda;

import java.util.List;

public interface DeudaRepository extends JpaRepository<Deuda, Long> {
    List<Deuda> findByEstudianteIdAndPeriodo(Long estudianteId, String periodo);
}
