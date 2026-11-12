package pe.sdj.matricula.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.sdj.matricula.domain.EstadoSolicitud;
import pe.sdj.matricula.domain.SolicitudModificacion;
import pe.sdj.matricula.domain.TipoSolicitud;

public interface SolicitudRepository extends JpaRepository<SolicitudModificacion, Long>,
        JpaSpecificationExecutor<SolicitudModificacion> {

    boolean existsByEstudianteIdAndTipoSolicitudAndEstado(Long estudianteId, TipoSolicitud tipo, EstadoSolicitud estado);

    @Override
    @EntityGraph(attributePaths = {"estudiante"})
    Page<SolicitudModificacion> findAll(Specification<SolicitudModificacion> spec, Pageable pageable);
}
