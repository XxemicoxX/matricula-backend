package pe.sdj.matricula.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.sdj.matricula.domain.Estudiante;

import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    /** Bloqueo pesimista: serializa las reservas concurrentes de un mismo estudiante. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Estudiante e where e.id = :id")
    Optional<Estudiante> findByIdForUpdate(@Param("id") Long id);

    Page<Estudiante> findByApoderadoId(Long apoderadoId, Pageable pageable);

    @Query("""
            select e from Estudiante e
            where lower(e.dni) like lower(concat('%', :q, '%'))
               or lower(e.nombres) like lower(concat('%', :q, '%'))
               or lower(e.apellidos) like lower(concat('%', :q, '%'))
            """)
    Page<Estudiante> buscar(@Param("q") String q, Pageable pageable);

    boolean existsByDniAndIdNot(String dni, Long id);
}
