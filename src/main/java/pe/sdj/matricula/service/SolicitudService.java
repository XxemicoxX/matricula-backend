package pe.sdj.matricula.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.sdj.matricula.common.BusinessException;
import pe.sdj.matricula.domain.*;
import pe.sdj.matricula.dto.SolicitudDtos.*;
import pe.sdj.matricula.repository.DocumentoRepository;
import pe.sdj.matricula.repository.EstudianteRepository;
import pe.sdj.matricula.repository.SolicitudRepository;
import pe.sdj.matricula.repository.UsuarioRepository;
import pe.sdj.matricula.security.UsuarioActual;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Solicitudes de cambio de datos fijos (RN-04) y casos excepcionales (RN-10). Mismo flujo:
 * Apoderado registra con sustento -> queda PENDIENTE -> Secretaría aprueba o rechaza -> se notifica.
 */
@Service
@RequiredArgsConstructor
public class SolicitudService {

    private static final Set<String> CAMPOS_FIJOS = Set.of("dni", "nombres", "apellidos", "fechaNacimiento", "genero");
    private static final int MAX_DOCUMENTOS = 5;

    private final SolicitudRepository solicitudes;
    private final DocumentoRepository documentos;
    private final EstudianteRepository estudiantes;
    private final UsuarioRepository usuarios;
    private final AlmacenamientoService almacenamiento;
    private final NotificacionService notificaciones;
    private final AuditoriaService auditoria;
    private final ObjectMapper om;
    private final Clock clock;

    @Transactional
    public SolicitudDto crearCambioDatos(UsuarioActual u, CrearSolicitudRequest req, List<MultipartFile> archivos) {
        Estudiante e = estudianteAutorizado(req.estudianteId(), u);
        Map<String, String> campos = validarCampos(req.campos());

        SolicitudModificacion s = nueva(e, u, TipoSolicitud.CAMBIO_DATOS);
        s.setDescripcion(req.justificacion().trim());
        s.setDatosSolicitados(aJson(campos));
        return guardarConDocumentos(s, archivos, "Solicitud de cambio de datos registrada");
    }

    @Transactional
    public SolicitudDto crearCasoExcepcional(UsuarioActual u, CrearCasoRequest req, List<MultipartFile> archivos) {
        Estudiante e = estudianteAutorizado(req.estudianteId(), u);
        SolicitudModificacion s = nueva(e, u, TipoSolicitud.CASO_EXCEPCIONAL);
        s.setMotivo(req.motivo().name());
        s.setDescripcion(req.descripcion().trim());
        return guardarConDocumentos(s, archivos, "Caso excepcional registrado");
    }

    /** Apoderado: sus solicitudes (CU-27). Personal: según permiso, con filtros opcionales. */
    @Transactional(readOnly = true)
    public Page<SolicitudDto> listar(UsuarioActual u, TipoSolicitud tipo, EstadoSolicitud estado, Pageable pageable) {
        Set<TipoSolicitud> visibles = tiposVisibles(u);
        if (visibles.isEmpty() || (tipo != null && !visibles.contains(tipo))) {
            throw BusinessException.forbidden("SIN_PERMISOS",
                    "No tienes los permisos necesarios para acceder a esta opción.");
        }
        Specification<SolicitudModificacion> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (u.esApoderado()) {
                ps.add(cb.equal(root.get("estudiante").get("apoderado").get("id"), u.apoderadoId()));
            }
            ps.add(tipo != null ? cb.equal(root.get("tipoSolicitud"), tipo) : root.get("tipoSolicitud").in(visibles));
            if (estado != null) ps.add(cb.equal(root.get("estado"), estado));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return solicitudes.findAll(spec, pageable).map(this::aDto);
    }

    @Transactional(readOnly = true)
    public SolicitudDto obtener(Long id, UsuarioActual u) {
        return aDto(cargarVisible(id, u));
    }

    @Transactional
    public SolicitudDto resolver(Long id, ResolverRequest req, UsuarioActual u) {
        SolicitudModificacion s = solicitudes.findById(id)
                .orElseThrow(() -> BusinessException.notFound("La solicitud no existe."));
        if (u.esApoderado() || !puedeResolver(u, s.getTipoSolicitud())) {
            throw BusinessException.forbidden("SIN_PERMISOS", "No tienes los permisos necesarios para acceder a esta opción.");
        }
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw BusinessException.conflict("SOLICITUD_YA_RESUELTA", "La solicitud ya fue resuelta.");
        }
        boolean aprobar = req.decision() == Decision.APROBAR;
        if (!aprobar && (req.observacion() == null || req.observacion().isBlank())) {
            throw BusinessException.badRequest("OBSERVACION_REQUERIDA", "Indica el motivo del rechazo.");
        }

        Estudiante e = s.getEstudiante();
        if (aprobar) {
            if (s.getTipoSolicitud() == TipoSolicitud.CAMBIO_DATOS) {
                aplicarCambios(e, leerJson(s.getDatosSolicitados()));
            } else {
                e.setExcepcionAprobada(true); // RN-10: se libera el flujo de matrícula
            }
        }
        s.setEstado(aprobar ? EstadoSolicitud.APROBADA : EstadoSolicitud.RECHAZADA);
        s.setRevisor(usuarios.getReferenceById(u.id()));
        s.setFechaRespuesta(clock.instant());
        s.setObservacion(req.observacion());

        String tipoTxt = s.getTipoSolicitud() == TipoSolicitud.CAMBIO_DATOS ? "cambio de datos" : "caso excepcional";
        auditoria.registrar(aprobar ? "SOLICITUD_APROBADA" : "SOLICITUD_RECHAZADA",
                "Solicitud " + s.getId() + " (" + tipoTxt + ") estudiante " + e.getId());
        notificaciones.notificarApoderado(e.getApoderado(), null,
                aprobar ? "SOLICITUD_APROBADA" : "SOLICITUD_RECHAZADA",
                "Tu solicitud de " + tipoTxt + " para " + e.nombreCompleto() + " fue "
                        + (aprobar ? "aprobada." : "rechazada. Motivo: " + req.observacion()));
        return aDto(s);
    }

    @Transactional(readOnly = true)
    public Resource documento(Long solicitudId, Long documentoId, UsuarioActual u) {
        cargarVisible(solicitudId, u);
        Documento d = documentos.findById(documentoId)
                .filter(x -> x.getSolicitud().getId().equals(solicitudId))
                .orElseThrow(() -> BusinessException.notFound("El documento no existe."));
        return almacenamiento.cargar(d.getRutaArchivo());
    }

    @Transactional(readOnly = true)
    public Documento infoDocumento(Long solicitudId, Long documentoId) {
        return documentos.findById(documentoId)
                .filter(x -> x.getSolicitud().getId().equals(solicitudId))
                .orElseThrow(() -> BusinessException.notFound("El documento no existe."));
    }

    // ---- helpers ----

    private Estudiante estudianteAutorizado(Long id, UsuarioActual u) {
        Estudiante e = estudiantes.findById(id).orElseThrow(() -> BusinessException.notFound("El estudiante no existe."));
        MatriculaService.verificarPropietario(e, u);
        return e;
    }

    private SolicitudModificacion nueva(Estudiante e, UsuarioActual u, TipoSolicitud tipo) {
        if (solicitudes.existsByEstudianteIdAndTipoSolicitudAndEstado(e.getId(), tipo, EstadoSolicitud.PENDIENTE)) {
            throw BusinessException.conflict("SOLICITUD_PENDIENTE",
                    "Ya tienes una solicitud de este tipo en revisión para el estudiante.");
        }
        SolicitudModificacion s = new SolicitudModificacion();
        s.setEstudiante(e);
        s.setSolicitante(usuarios.getReferenceById(u.id()));
        s.setTipoSolicitud(tipo);
        s.setEstado(EstadoSolicitud.PENDIENTE);
        s.setFechaSolicitud(clock.instant());
        return s;
    }

    private SolicitudDto guardarConDocumentos(SolicitudModificacion s, List<MultipartFile> archivos, String accion) {
        List<MultipartFile> validos = archivos == null ? List.of()
                : archivos.stream().filter(a -> a != null && !a.isEmpty()).toList();
        if (validos.isEmpty()) {
            throw BusinessException.badRequest("SUSTENTO_REQUERIDO", "Adjunta al menos un documento de sustento (PDF o imagen).");
        }
        if (validos.size() > MAX_DOCUMENTOS) {
            throw BusinessException.badRequest("DEMASIADOS_ARCHIVOS", "Puedes adjuntar hasta " + MAX_DOCUMENTOS + " documentos.");
        }
        s = solicitudes.save(s);
        for (MultipartFile f : validos) {
            AlmacenamientoService.Guardado g = almacenamiento.guardar(f, "solicitudes/" + s.getId());
            Documento d = new Documento();
            d.setSolicitud(s);
            d.setNombreArchivo(g.nombreOriginal());
            d.setRutaArchivo(g.rutaRelativa());
            d.setTipoDocumento(g.contentType());
            d.setFechaSubida(clock.instant());
            documentos.save(d);
        }
        auditoria.registrar("SOLICITUD_REGISTRADA", accion + " (id " + s.getId() + ", estudiante "
                + s.getEstudiante().getId() + ")");
        notificaciones.notificarRol(Roles.SECRETARIA, null, "NUEVA_SOLICITUD",
                "Nueva solicitud pendiente (" + s.getTipoSolicitud() + ") del estudiante " + s.getEstudiante().nombreCompleto() + ".");
        return aDto(s);
    }

    private SolicitudModificacion cargarVisible(Long id, UsuarioActual u) {
        SolicitudModificacion s = solicitudes.findById(id)
                .orElseThrow(() -> BusinessException.notFound("La solicitud no existe."));
        if (u.esApoderado()) {
            if (!s.getEstudiante().getApoderado().getId().equals(u.apoderadoId())) {
                throw BusinessException.notFound("La solicitud no existe.");
            }
        } else if (!tiposVisibles(u).contains(s.getTipoSolicitud())) {
            throw BusinessException.forbidden("SIN_PERMISOS", "No tienes los permisos necesarios para acceder a esta opción.");
        }
        return s;
    }

    /** Qué tipos de solicitud puede consultar el usuario (apoderado: ambos; Secretaría: ambos). */
    private Set<TipoSolicitud> tiposVisibles(UsuarioActual u) {
        if (u.esApoderado() || Roles.SECRETARIA.equals(u.rol())) {
            return EnumSet.allOf(TipoSolicitud.class);
        }
        return EnumSet.noneOf(TipoSolicitud.class);
    }

    private boolean puedeResolver(UsuarioActual u, TipoSolicitud tipo) {
        return Roles.SECRETARIA.equals(u.rol());
    }

    private Map<String, String> validarCampos(Map<String, String> campos) {
        if (campos == null || campos.isEmpty()) {
            throw BusinessException.badRequest("SOLICITUD_INVALIDA", "Indica los datos que deseas modificar.");
        }
        Map<String, String> limpio = new LinkedHashMap<>();
        campos.forEach((k, v) -> {
            if (!CAMPOS_FIJOS.contains(k)) {
                throw BusinessException.badRequest("CAMPO_NO_PERMITIDO", "El campo '" + k + "' no puede solicitarse por esta vía.");
            }
            if (v == null || v.isBlank() || v.length() > 100) {
                throw BusinessException.badRequest("SOLICITUD_INVALIDA", "El valor del campo '" + k + "' es inválido.");
            }
            if ("fechaNacimiento".equals(k)) {
                try {
                    LocalDate.parse(v.trim());
                } catch (DateTimeParseException ex) {
                    throw BusinessException.badRequest("SOLICITUD_INVALIDA", "La fecha de nacimiento debe tener el formato AAAA-MM-DD.");
                }
            }
            limpio.put(k, v.trim());
        });
        return limpio;
    }

    private void aplicarCambios(Estudiante e, Map<String, String> campos) {
        campos.forEach((k, v) -> {
            switch (k) {
                case "dni" -> {
                    if (estudiantes.existsByDniAndIdNot(v, e.getId())) {
                        throw BusinessException.conflict("DNI_DUPLICADO", "Ya existe otro estudiante con ese DNI.");
                    }
                    e.setDni(v);
                }
                case "nombres" -> e.setNombres(v);
                case "apellidos" -> e.setApellidos(v);
                case "fechaNacimiento" -> e.setFechaNacimiento(LocalDate.parse(v));
                case "genero" -> e.setGenero(v);
                default -> {
                }
            }
        });
    }

    private String aJson(Map<String, String> m) {
        try {
            return om.writeValueAsString(m);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Map<String, String> leerJson(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return om.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private SolicitudDto aDto(SolicitudModificacion s) {
        Estudiante e = s.getEstudiante();
        List<DocumentoDto> docs = documentos.findBySolicitudIdOrderByIdAsc(s.getId()).stream()
                .map(d -> new DocumentoDto(d.getId(), d.getNombreArchivo(), d.getTipoDocumento(), d.getFechaSubida())).toList();
        return new SolicitudDto(s.getId(), e.getId(), e.nombreCompleto(), e.getDni(), s.getTipoSolicitud(), s.getMotivo(),
                s.getDescripcion(), leerJson(s.getDatosSolicitados()), s.getEstado(), s.getFechaSolicitud(),
                s.getFechaRespuesta(), s.getObservacion(), docs);
    }
}
