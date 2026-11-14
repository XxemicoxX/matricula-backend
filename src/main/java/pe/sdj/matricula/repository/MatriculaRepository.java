package pe.sdj.matricula.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.sdj.matricula.domain.EstadoMatricula;
import pe.sdj.matricula.domain.Matricula;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MatriculaRepository extends JpaRepository<Matricula, Long>, JpaSpecificationExecutor<Matricula> {

    String OCUPA = "(m.estado = pe.sdj.matricula.domain.EstadoMatricula.MATRICULADO "
            + "or (m.estado = pe.sdj.matricula.domain.EstadoMatricula.RESERVA_PENDIENTE and m.fechaExpiracion > :ahora))";

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Matricula m where m.id = :id")
    Optional<Matricula> findByIdForUpdate(@Param("id") Long id);

    /** Cupos ocupados = matriculados + reservas vigentes (RN-03). */
    @Query("select count(m) from Matricula m where m.seccion.id = :seccionId and m.periodoAcademico = :periodo and " + OCUPA)
    long contarOcupadas(@Param("seccionId") Long seccionId, @Param("periodo") String periodo, @Param("ahora") Instant ahora);

    /** Matrículas confirmadas de una sección (para el cierre automático al completarse el aforo). */
    long countBySeccionIdAndPeriodoAcademicoAndEstado(Long seccionId, String periodoAcademico, EstadoMatricula estado);

    /** Filas [seccionId, estado, cantidad] de cupos ocupados, para listar disponibilidad sin N+1. */
    @Query("select m.seccion.id, m.estado, count(m) from Matricula m where m.periodoAcademico = :periodo and " + OCUPA
            + " group by m.seccion.id, m.estado")
    List<Object[]> contarOcupadasPorSeccion(@Param("periodo") String periodo, @Param("ahora") Instant ahora);

    Optional<Matricula> findFirstByEstudianteIdAndPeriodoAcademicoAndEstadoOrderByIdDesc(
            Long estudianteId, String periodo, EstadoMatricula estado);

    long countByEstudianteIdAndPeriodoAcademico(Long estudianteId, String periodo);

    List<Matricula> findByEstudianteIdOrderByIdDesc(Long estudianteId);

    List<Matricula> findTop200ByEstadoAndFechaExpiracionLessThanEqual(EstadoMatricula estado, Instant limite);

    long countByEstadoAndPeriodoAcademicoAndFechaMatriculaGreaterThanEqualAndFechaMatriculaLessThan(
            EstadoMatricula estado, String periodo, Instant desde, Instant hasta);

    long countByEstadoAndPeriodoAcademico(EstadoMatricula estado, String periodo);

    @Override
    @EntityGraph(attributePaths = {"estudiante", "seccion", "seccion.grado"})
    Page<Matricula> findAll(Specification<Matricula> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"estudiante", "seccion", "seccion.grado"})
    List<Matricula> findAll(Specification<Matricula> spec);
}
