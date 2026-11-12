package pe.sdj.matricula.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.sdj.matricula.domain.Auditoria;
import pe.sdj.matricula.dto.AdminDtos.AuditoriaDto;
import pe.sdj.matricula.repository.AuditoriaRepository;
import pe.sdj.matricula.repository.UsuarioRepository;
import pe.sdj.matricula.security.SecurityUtils;
import pe.sdj.matricula.security.UsuarioActual;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Registro de operaciones relevantes (usuario, fecha/hora, IP y acción) - documento 11.1 "Auditoría". */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository repo;
    private final UsuarioRepository usuarios;
    private final Clock clock;

    /** Registra la acción a nombre del usuario autenticado en la petición actual (o sin usuario, p. ej. tareas automáticas). */
    @Transactional
    public void registrar(String accion, String descripcion) {
        Long uid = SecurityUtils.actual().map(UsuarioActual::id).orElse(null);
        registrarComo(uid, accion, descripcion);
    }

    @Transactional
    public void registrarComo(Long usuarioId, String accion, String descripcion) {
        Auditoria a = new Auditoria();
        if (usuarioId != null) {
            a.setUsuario(usuarios.getReferenceById(usuarioId));
        }
        a.setAccion(accion);
        a.setFechaHora(clock.instant());
        a.setDireccionIp(ipActual());
        a.setDescripcion(descripcion == null ? null : descripcion.substring(0, Math.min(255, descripcion.length())));
        repo.save(a);
    }

    @Transactional(readOnly = true)
    public Page<AuditoriaDto> consultar(Long usuarioId, String accion, Instant desde, Instant hasta, Pageable pageable) {
        Specification<Auditoria> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (usuarioId != null) ps.add(cb.equal(root.get("usuario").get("id"), usuarioId));
            if (accion != null && !accion.isBlank()) ps.add(cb.equal(root.get("accion"), accion.trim()));
            if (desde != null) ps.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), desde));
            if (hasta != null) ps.add(cb.lessThan(root.get("fechaHora"), hasta));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return repo.findAll(spec, pageable).map(a -> new AuditoriaDto(a.getId(),
                a.getUsuario() == null ? null : a.getUsuario().getId(),
                a.getUsuario() == null ? null : a.getUsuario().nombreCompleto(),
                a.getAccion(), a.getFechaHora(), a.getDireccionIp(), a.getDescripcion()));
    }

    private String ipActual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getRemoteAddr();
        }
        return null;
    }
}
