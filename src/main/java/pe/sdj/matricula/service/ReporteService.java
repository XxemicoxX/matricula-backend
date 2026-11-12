package pe.sdj.matricula.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sdj.matricula.common.BusinessException;
import pe.sdj.matricula.config.AppProperties;
import pe.sdj.matricula.domain.EstadoMatricula;
import pe.sdj.matricula.domain.Matricula;
import pe.sdj.matricula.dto.AdminDtos.FilaReporteDto;
import pe.sdj.matricula.repository.MatriculaRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** CU-21/CU-22: reportes de matrícula con filtros y exportación a Excel. */
@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final int MAX_EXPORTACION = 20000;

    private final MatriculaRepository matriculas;
    private final AuditoriaService auditoria;
    private final AppProperties props;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<FilaReporteDto> consultar(String periodo, Long gradoId, Long seccionId, EstadoMatricula estado,
                                          LocalDate desde, LocalDate hasta, Pageable pageable) {
        validarRango(desde, hasta);
        return matriculas.findAll(spec(periodo, gradoId, seccionId, estado, desde, hasta), pageable).map(this::fila);
    }

    @Transactional
    public byte[] exportar(String periodo, Long gradoId, Long seccionId, EstadoMatricula estado,
                           LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<Matricula> lista = matriculas.findAll(spec(periodo, gradoId, seccionId, estado, desde, hasta));
        if (lista.size() > MAX_EXPORTACION) {
            throw BusinessException.badRequest("REPORTE_MUY_GRANDE", "El reporte supera " + MAX_EXPORTACION
                    + " filas. Aplica más filtros.");
        }
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = wb.createSheet("Matrículas");
            CellStyle negrita = wb.createCellStyle();
            Font f = wb.createFont();
            f.setBold(true);
            negrita.setFont(f);
            String[] cab = {"N° matrícula", "Estudiante", "DNI", "Grado", "Sección", "Fecha", "Estado"};
            Row r0 = hoja.createRow(0);
            for (int i = 0; i < cab.length; i++) {
                Cell c = r0.createCell(i);
                c.setCellValue(cab[i]);
                c.setCellStyle(negrita);
            }
            int i = 1;
            for (Matricula m : lista) {
                FilaReporteDto d = fila(m);
                Row r = hoja.createRow(i++);
                r.createCell(0).setCellValue(d.matriculaId());
                r.createCell(1).setCellValue(seguro(d.estudiante()));
                r.createCell(2).setCellValue(seguro(d.dni()));
                r.createCell(3).setCellValue(seguro(d.grado()));
                r.createCell(4).setCellValue(seguro(d.seccion()));
                r.createCell(5).setCellValue(d.fecha() == null ? "" : d.fecha().toString());
                r.createCell(6).setCellValue(d.estado().name());
            }
            for (int c = 0; c < cab.length; c++) hoja.autoSizeColumn(c);
            wb.write(out);
            auditoria.registrar("EXPORTAR_REPORTE", "Reporte de matrículas exportado (" + lista.size() + " filas)");
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el Excel", e);
        }
    }

    /** Evita inyección de fórmulas al abrir el Excel. */
    private String seguro(String v) {
        if (v == null) return "";
        return (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) ? "'" + v : v;
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw BusinessException.badRequest("RANGO_INVALIDO", "La fecha inicial no puede ser posterior a la final.");
        }
    }

    private Specification<Matricula> spec(String periodo, Long gradoId, Long seccionId, EstadoMatricula estado,
                                          LocalDate desde, LocalDate hasta) {
        ZoneId zona = ZoneId.of(props.getZonaHoraria());
        String per = (periodo == null || periodo.isBlank()) ? props.getPeriodoActual() : periodo;
        return (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("periodoAcademico"), per));
            if (gradoId != null) ps.add(cb.equal(root.get("seccion").get("grado").get("id"), gradoId));
            if (seccionId != null) ps.add(cb.equal(root.get("seccion").get("id"), seccionId));
            if (estado != null) ps.add(cb.equal(root.get("estado"), estado));
            if (desde != null) ps.add(cb.greaterThanOrEqualTo(root.get("fechaReserva"), desde.atStartOfDay(zona).toInstant()));
            if (hasta != null) ps.add(cb.lessThan(root.get("fechaReserva"), hasta.plusDays(1).atStartOfDay(zona).toInstant()));
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private FilaReporteDto fila(Matricula m) {
        ZoneId zona = ZoneId.of(props.getZonaHoraria());
        var ins = m.getFechaMatricula() != null ? m.getFechaMatricula() : m.getFechaReserva();
        return new FilaReporteDto(m.getId(), m.getEstudiante().nombreCompleto(), m.getEstudiante().getDni(),
                m.getSeccion().getGrado().getNombre(), m.getSeccion().getNombre(),
                ins.atZone(zona).toLocalDate(), m.getEstado());
    }
}
