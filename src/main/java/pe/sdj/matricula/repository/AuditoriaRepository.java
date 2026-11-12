package pe.sdj.matricula.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.sdj.matricula.domain.Auditoria;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long>, JpaSpecificationExecutor<Auditoria> {
    @Override
    @EntityGraph(attributePaths = {"usuario"})
    Page<Auditoria> findAll(Specification<Auditoria> spec, Pageable pageable);
}
