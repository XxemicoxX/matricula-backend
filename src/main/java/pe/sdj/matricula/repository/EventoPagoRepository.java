package pe.sdj.matricula.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sdj.matricula.domain.EstadoEventoPago;
import pe.sdj.matricula.domain.EventoPago;

import java.util.Collection;
import java.util.List;

public interface EventoPagoRepository extends JpaRepository<EventoPago, Long> {
    List<EventoPago> findTop50ByEstadoInAndIntentosLessThanOrderByIdAsc(Collection<EstadoEventoPago> estados, int maxIntentos);
}
