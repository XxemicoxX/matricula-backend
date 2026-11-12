package pe.sdj.matricula.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sdj.matricula.domain.Grado;

import java.util.Optional;

public interface GradoRepository extends JpaRepository<Grado, Long> {
    Optional<Grado> findByNombreAndNivel(String nombre, String nivel);
}
