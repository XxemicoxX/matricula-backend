package pe.sdj.matricula.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.sdj.matricula.domain.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    Page<Notificacion> findByApoderadoIdOrderByFechaEnvioDesc(Long apoderadoId, Pageable pageable);

    Page<Notificacion> findByRolDestinoOrderByFechaEnvioDesc(String rolDestino, Pageable pageable);
}
