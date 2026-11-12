package pe.sdj.matricula.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sdj.matricula.common.BusinessException;
import pe.sdj.matricula.domain.CondicionEstudiante;
import pe.sdj.matricula.domain.EstadoSolicitud;
import pe.sdj.matricula.domain.Estudiante;
import pe.sdj.matricula.domain.TipoSolicitud;
import pe.sdj.matricula.repository.SolicitudRepository;

/** Condiciones previas para ver secciones y reservar vacante (RN-10, RN-01, RN-06). */
@Service
@RequiredArgsConstructor
public class ReglasMatriculaService {

    private final SolicitudRepository solicitudes;
    private final DeudaService deudas;
    private final MineduService minedu;

    @Transactional
    public void validarElegibilidad(Estudiante e) {
        // RN-10: un caso excepcional detiene el flujo regular hasta que Secretaría lo apruebe.
        if (solicitudes.existsByEstudianteIdAndTipoSolicitudAndEstado(
                e.getId(), TipoSolicitud.CASO_EXCEPCIONAL, EstadoSolicitud.PENDIENTE)) {
            throw BusinessException.unprocessable("CASO_EXCEPCIONAL_PENDIENTE",
                    "Tu caso excepcional está en revisión. Podrás continuar cuando Secretaría lo apruebe.");
        }
        if (e.getCondicion() == CondicionEstudiante.NO_REGULAR && !e.isExcepcionAprobada()) {
            throw BusinessException.unprocessable("REQUIERE_CASO_EXCEPCIONAL",
                    "La matrícula de este estudiante requiere aprobación previa. Registra una solicitud de caso excepcional.");
        }
        deudas.verificarSinDeudas(e);   // RN-01
        minedu.asegurarValidado(e);     // RN-06
    }
}
