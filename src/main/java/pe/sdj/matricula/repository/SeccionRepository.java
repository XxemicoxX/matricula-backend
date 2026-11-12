package pe.sdj.matricula.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.sdj.matricula.domain.Seccion;

import java.util.List;
import java.util.Optional;

public interface SeccionRepository extends JpaRepository<Seccion, Long> {

    /** Bloqueo pesimista de la sección: el conteo de aforo y la reserva ocurren de forma atómica (RN-03). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seccion s join fetch s.grado where s.id = :id")
    Optional<Seccion> findByIdForUpdate(@Param("id") Long id);

    @Query("select s from Seccion s join fetch s.grado g where (:gradoId is null or g.id = :gradoId) order by g.nivel, g.nombre, s.nombre")
    List<Seccion> listar(@Param("gradoId") Long gradoId);

    boolean existsByGradoIdAndNombreIgnoreCase(Long gradoId, String nombre);
}
