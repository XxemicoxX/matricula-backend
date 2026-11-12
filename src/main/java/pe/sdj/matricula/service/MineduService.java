package pe.sdj.matricula.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sdj.matricula.common.BusinessException;
import pe.sdj.matricula.config.AppProperties;
import pe.sdj.matricula.domain.Estudiante;
import pe.sdj.matricula.domain.ResultadoMinedu;
import pe.sdj.matricula.domain.ValidacionMinedu;
import pe.sdj.matricula.dto.EstudianteDtos.ValidacionMineduDto;
import pe.sdj.matricula.integration.MineduClient;
import pe.sdj.matricula.integration.MineduClient.ResultadoValidacion;
import pe.sdj.matricula.repository.ValidacionMineduRepository;

import java.time.Clock;
import java.util.Optional;

/** RN-06: la identidad oficial del estudiante debe validarse con MINEDU antes de elegir sección. */
@Service
@RequiredArgsConstructor
public class MineduService {

    private final MineduClient minedu;
    private final ValidacionMineduRepository validaciones;
    private final AppProperties props;
    private final Clock clock;

    /** Fuerza una validación nueva (p. ej. tras guardar cambios de datos) y la registra. */
    @Transactional
    public ValidacionMineduDto validar(Estudiante e) {
        ResultadoValidacion r = minedu.validar(e.getDni(), e.getNombres(), e.getApellidos(), e.getFechaNacimiento());
        ValidacionMinedu v = new ValidacionMinedu();
        v.setEstudiante(e);
        v.setPeriodoAcademico(props.getPeriodoActual());
        v.setFechaValidacion(clock.instant());
        v.setResultado(r.valido() ? ResultadoMinedu.VALIDO : ResultadoMinedu.OBSERVADO);
        v.setObservacion(r.observacion());
        validaciones.save(v);
        return new ValidacionMineduDto(v.getResultado(), v.getObservacion(), v.getFechaValidacion());
    }

    /** Exige una validación VALIDA del periodo vigente; si no hay una, la solicita al MINEDU. */
    @Transactional
    public void asegurarValidado(Estudiante e) {
        Optional<ValidacionMinedu> ultima = validaciones
                .findFirstByEstudianteIdAndPeriodoAcademicoOrderByIdDesc(e.getId(), props.getPeriodoActual());
        if (ultima.isPresent() && ultima.get().getResultado() == ResultadoMinedu.VALIDO) {
            return;
        }
        ValidacionMineduDto r = validar(e);
        if (r.resultado() != ResultadoMinedu.VALIDO) {
            throw BusinessException.unprocessable("MINEDU_OBSERVADO",
                    "No se pudo validar la información del estudiante con el MINEDU. Comunícate con Secretaría.");
        }
    }
}
