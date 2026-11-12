package pe.sdj.matricula.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sdj.matricula.common.BusinessException;
import pe.sdj.matricula.domain.Apoderado;
import pe.sdj.matricula.domain.Estudiante;
import pe.sdj.matricula.domain.Grado;
import pe.sdj.matricula.dto.EstudianteDtos.*;
import pe.sdj.matricula.repository.ApoderadoRepository;
import pe.sdj.matricula.repository.EstudianteRepository;
import pe.sdj.matricula.security.UsuarioActual;

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudiantes;
    private final ApoderadoRepository apoderados;
    private final MatriculaService matriculas;
    private final DeudaService deudas;
    private final MineduService minedu;
    private final AuditoriaService auditoria;

    /** Apoderado: sus estudiantes. Personal: búsqueda por DNI o nombre (parámetro q). */
    @Transactional(readOnly = true)
    public Page<EstudianteDto> listar(UsuarioActual u, String q, Pageable pageable) {
        Page<Estudiante> page = u.esApoderado()
                ? estudiantes.findByApoderadoId(u.apoderadoId(), pageable)
                : estudiantes.buscar(q == null ? "" : q.trim(), pageable);
        return page.map(this::aDto);
    }

    @Transactional(readOnly = true)
    public EstudianteDto obtener(Long id, UsuarioActual u) {
        return aDto(cargar(id, u));
    }

    /**
     * CU-4: el apoderado actualiza libremente teléfono, dirección y correo (RN-04). Se aplican al registro del
     * apoderado y al del estudiante, que comparten el mismo domicilio/contacto en el prototipo.
     */
    @Transactional
    public EstudianteDto actualizarContacto(Long id, ContactoRequest req, UsuarioActual u) {
        if (req.telefono() == null && req.direccion() == null && req.correo() == null) {
            throw BusinessException.badRequest("SOLICITUD_INVALIDA", "Indica al menos un dato de contacto a actualizar.");
        }
        Estudiante e = cargar(id, u);
        Apoderado a = e.getApoderado();
        if (req.telefono() != null) {
            e.setTelefono(req.telefono().trim());
            a.setTelefono(req.telefono().trim());
        }
        if (req.direccion() != null) {
            e.setDireccion(req.direccion().trim());
            a.setDireccion(req.direccion().trim());
        }
        if (req.correo() != null) {
            e.setCorreo(req.correo().trim());
            a.setCorreo(req.correo().trim());
        }
        auditoria.registrar("ACTUALIZAR_CONTACTO", "Contacto del estudiante " + e.getId());
        return aDto(e);
    }

    /** CU-3: consulta financiera a CUBICOL y registro del resultado. */
    @Transactional
    public ResultadoDeudasDto validarDeudas(Long id, UsuarioActual u) {
        ResultadoDeudasDto r = deudas.consultar(cargar(id, u));
        auditoria.registrar("CONSULTA_DEUDAS", "Estudiante " + id + (r.puedeContinuar() ? ": sin deudas" : ": con deudas"));
        return r;
    }

    /** CU-7: validación con MINEDU. */
    @Transactional
    public ValidacionMineduDto validarMinedu(Long id, UsuarioActual u) {
        ValidacionMineduDto r = minedu.validar(cargar(id, u));
        auditoria.registrar("VALIDACION_MINEDU", "Estudiante " + id + ": " + r.resultado());
        return r;
    }

    private Estudiante cargar(Long id, UsuarioActual u) {
        Estudiante e = estudiantes.findById(id).orElseThrow(() -> BusinessException.notFound(
                "La información que buscas no se encuentra disponible o no existe."));
        MatriculaService.verificarPropietario(e, u);
        return e;
    }

    private EstudianteDto aDto(Estudiante e) {
        Apoderado a = e.getApoderado();
        Grado g = e.getGradoDestino();
        return new EstudianteDto(e.getId(), e.getDni(), e.getNombres(), e.getApellidos(), e.getFechaNacimiento(),
                e.getGenero(), e.getCorreo(), e.getTelefono(), e.getDireccion(), e.getCondicion(), e.isExcepcionAprobada(),
                g == null ? null : new GradoDto(g.getId(), g.getNombre(), g.getNivel(), g.getMontoMatricula()),
                new ApoderadoDto(a.getId(), a.getDni(), a.getNombres(), a.getApellidos(), a.getTelefono(), a.getDireccion(), a.getCorreo()),
                matriculas.vigente(e.getId()));
    }
}
