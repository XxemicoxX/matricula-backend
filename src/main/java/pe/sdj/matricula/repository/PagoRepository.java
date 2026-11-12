package pe.sdj.matricula.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sdj.matricula.domain.EstadoPago;
import pe.sdj.matricula.domain.Pago;

import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByProveedorAndNumeroOperacion(String proveedor, String numeroOperacion);

    Optional<Pago> findFirstByCodigoPagoIdAndEstadoOrderByIdDesc(Long codigoPagoId, EstadoPago estado);
}
