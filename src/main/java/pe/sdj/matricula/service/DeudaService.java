package pe.sdj.matricula.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sdj.matricula.common.BusinessException;
import pe.sdj.matricula.config.AppProperties;
import pe.sdj.matricula.domain.Deuda;
import pe.sdj.matricula.domain.EstadoDeuda;
import pe.sdj.matricula.domain.Estudiante;
import pe.sdj.matricula.dto.EstudianteDtos.DeudaDto;
import pe.sdj.matricula.dto.EstudianteDtos.ResultadoDeudasDto;
import pe.sdj.matricula.integration.CubicolClient;
import pe.sdj.matricula.integration.CubicolClient.DeudaExterna;
import pe.sdj.matricula.repository.DeudaRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** RN-01: consulta en tiempo real a CUBICOL las deudas del periodo anterior y las deja registradas. */
@Service
@RequiredArgsConstructor
public class DeudaService {

    private final CubicolClient cubicol;
    private final DeudaRepository deudas;
    private final AppProperties props;
    private final Clock clock;

    /** Sincroniza las deudas del periodo anterior con CUBICOL y devuelve el resultado. */
    @Transactional
    public ResultadoDeudasDto consultar(Estudiante e) {
        String periodo = props.periodoAnterior();
        List<DeudaExterna> externas = cubicol.consultarDeudas(e.getDni(), periodo);
        Instant ahora = clock.instant();

        Map<String, Deuda> existentes = deudas.findByEstudianteIdAndPeriodo(e.getId(), periodo).stream()
                .collect(Collectors.toMap(Deuda::getConcepto, d -> d, (a, b) -> a, HashMap::new));
        Set<String> vigentes = externas.stream().map(DeudaExterna::concepto).collect(Collectors.toSet());

        for (DeudaExterna ext : externas) {
            Deuda d = existentes.get(ext.concepto());
            if (d == null) {
                d = new Deuda();
                d.setEstudiante(e);
                d.setPeriodo(periodo);
                d.setConcepto(ext.concepto());
                existentes.put(ext.concepto(), d);
            }
            d.setMonto(ext.monto());
            d.setEstado(EstadoDeuda.PENDIENTE);
            d.setFechaConsulta(ahora);
            deudas.save(d);
        }
        // Lo que ya no figura como pendiente en CUBICOL se considera pagado.
        existentes.forEach((concepto, d) -> {
            if (!vigentes.contains(concepto) && d.getEstado() == EstadoDeuda.PENDIENTE) {
                d.setEstado(EstadoDeuda.PAGADA);
                d.setFechaConsulta(ahora);
            }
        });

        List<DeudaDto> dtos = existentes.values().stream()
                .map(d -> new DeudaDto(d.getPeriodo(), d.getConcepto(), d.getMonto(), d.getEstado())).toList();
        BigDecimal total = externas.stream().map(DeudaExterna::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResultadoDeudasDto(externas.isEmpty(), periodo, total, dtos);
    }

    /** Bloquea el proceso (422) si el estudiante tiene deudas del año anterior. */
    @Transactional
    public void verificarSinDeudas(Estudiante e) {
        ResultadoDeudasDto r = consultar(e);
        if (!r.puedeContinuar()) {
            throw BusinessException.unprocessable("DEUDA_PENDIENTE",
                    "No se puede completar el proceso. Por favor, verifica que no tengas deudas pendientes.");
        }
    }
}
