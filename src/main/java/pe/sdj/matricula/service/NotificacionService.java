package pe.sdj.matricula.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sdj.matricula.common.BusinessException;
import pe.sdj.matricula.domain.Apoderado;
import pe.sdj.matricula.domain.Matricula;
import pe.sdj.matricula.domain.Notificacion;
import pe.sdj.matricula.dto.AdminDtos.NotificacionDto;
import pe.sdj.matricula.integration.NotificadorExterno;
import pe.sdj.matricula.repository.NotificacionRepository;
import pe.sdj.matricula.security.UsuarioActual;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository repo;
    private final NotificadorExterno notificador;
    private final Clock clock;

    /** Aviso a un apoderado (correo, y a la vez visible en su bandeja dentro del sistema). */
    @Transactional
    public void notificarApoderado(Apoderado apoderado, Matricula matricula, String tipo, String mensaje) {
        Notificacion n = nueva(tipo, "EMAIL", mensaje);
        n.setApoderado(apoderado);
        n.setMatricula(matricula);
        n.setDestinatario(apoderado.getCorreo());
        boolean ok = apoderado.getCorreo() != null && notificador.enviar("EMAIL", apoderado.getCorreo(), tipo, mensaje);
        n.setEstado(ok ? "ENVIADA" : "PENDIENTE");
        repo.save(n);
    }

    /** Aviso interno a un rol del personal (p. ej. TESORERIA/SECRETARIA ante pagos tardíos, RN-09). */
    @Transactional
    public void notificarRol(String rol, Matricula matricula, String tipo, String mensaje) {
        Notificacion n = nueva(tipo, "INTERNA", mensaje);
        n.setRolDestino(rol);
        n.setMatricula(matricula);
        n.setEstado("ENVIADA");
        repo.save(n);
    }

    @Transactional(readOnly = true)
    public Page<NotificacionDto> misNotificaciones(UsuarioActual u, Pageable pageable) {
        Page<Notificacion> page = u.esApoderado()
                ? repo.findByApoderadoIdOrderByFechaEnvioDesc(u.apoderadoId(), pageable)
                : repo.findByRolDestinoOrderByFechaEnvioDesc(u.rol(), pageable);
        return page.map(n -> new NotificacionDto(n.getId(), n.getTipo(), n.getCanal(), n.getMensaje(),
                n.getFechaEnvio(), n.isLeida(), n.getMatricula() == null ? null : n.getMatricula().getId()));
    }

    @Transactional
    public void marcarLeida(Long id, UsuarioActual u) {
        Notificacion n = repo.findById(id).orElseThrow(() -> BusinessException.notFound("La notificación no existe."));
        boolean propia = u.esApoderado()
                ? n.getApoderado() != null && n.getApoderado().getId().equals(u.apoderadoId())
                : u.rol().equals(n.getRolDestino());
        if (!propia) {
            throw BusinessException.notFound("La notificación no existe.");
        }
        n.setLeida(true);
    }

    private Notificacion nueva(String tipo, String canal, String mensaje) {
        Notificacion n = new Notificacion();
        n.setTipo(tipo);
        n.setCanal(canal);
        n.setMensaje(mensaje);
        n.setFechaEnvio(clock.instant());
        return n;
    }
}
