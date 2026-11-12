package pe.sdj.matricula.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sdj.matricula.domain.Apoderado;

import java.util.Optional;

public interface ApoderadoRepository extends JpaRepository<Apoderado, Long> {
    Optional<Apoderado> findByDni(String dni);
}
