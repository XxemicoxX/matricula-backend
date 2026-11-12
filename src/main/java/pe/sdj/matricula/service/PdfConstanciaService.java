package pe.sdj.matricula.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import pe.sdj.matricula.domain.CodigoPago;
import pe.sdj.matricula.domain.Estudiante;
import pe.sdj.matricula.domain.Matricula;
import pe.sdj.matricula.domain.Pago;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Genera la constancia de matrícula en PDF (CU-13). */
@Service
public class PdfConstanciaService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("es", "PE"));

    private final ZoneId zona;

    public PdfConstanciaService(@Value("${app.zona-horaria:America/Lima}") String zona) {
        this.zona = ZoneId.of(zona);
    }

    public byte[] generar(Matricula m, CodigoPago codigo, Pago pago) {
        Estudiante e = m.getEstudiante();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 56, 56, 64, 56);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font institucion = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font titulo = new Font(Font.HELVETICA, 20, Font.BOLD);
            Font etiqueta = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font valor = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font nota = new Font(Font.HELVETICA, 9, Font.ITALIC);

            Paragraph inst = new Paragraph("I.E.P. Santo Domingo de Jicamarca", institucion);
            inst.setAlignment(Element.ALIGN_CENTER);
            doc.add(inst);

            Paragraph tit = new Paragraph("CONSTANCIA DE MATRÍCULA", titulo);
            tit.setAlignment(Element.ALIGN_CENTER);
            tit.setSpacingBefore(14);
            tit.setSpacingAfter(6);
            doc.add(tit);

            Paragraph per = new Paragraph("Año académico " + m.getPeriodoAcademico(), institucion);
            per.setAlignment(Element.ALIGN_CENTER);
            per.setSpacingAfter(24);
            doc.add(per);

            PdfPTable t = new PdfPTable(new float[]{1.4f, 2.6f});
            t.setWidthPercentage(100);
            fila(t, "Constancia N°", String.format("%06d", m.getId()), etiqueta, valor);
            fila(t, "Estudiante", e.nombreCompleto(), etiqueta, valor);
            fila(t, "DNI del estudiante", e.getDni(), etiqueta, valor);
            fila(t, "Apoderado", e.getApoderado().getNombres() + " " + e.getApoderado().getApellidos(), etiqueta, valor);
            fila(t, "Nivel / Grado", m.getSeccion().getGrado().getNivel() + " - " + m.getSeccion().getGrado().getNombre(), etiqueta, valor);
            fila(t, "Sección", m.getSeccion().getNombre(), etiqueta, valor);
            fila(t, "Estado", "MATRICULADO", etiqueta, valor);
            fila(t, "Fecha de matrícula", fecha(m.getFechaMatricula()), etiqueta, valor);
            fila(t, "Código de pago", codigo.getCodigo(), etiqueta, valor);
            fila(t, "Monto pagado", "S/ " + codigo.getMonto().toPlainString(), etiqueta, valor);
            if (pago != null) {
                fila(t, "N° de operación", pago.getNumeroOperacion(), etiqueta, valor);
            }
            doc.add(t);

            Paragraph pie = new Paragraph("Documento generado por el sistema de matrícula. "
                    + "Emitido el " + fecha(Instant.now()) + ".", nota);
            pie.setSpacingBefore(28);
            doc.add(pie);
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar la constancia PDF", ex);
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }
        return out.toByteArray();
    }

    private void fila(PdfPTable t, String etiqueta, String valor, Font fe, Font fv) {
        PdfPCell a = new PdfPCell(new Phrase(etiqueta, fe));
        PdfPCell b = new PdfPCell(new Phrase(valor == null ? "-" : valor, fv));
        for (PdfPCell c : new PdfPCell[]{a, b}) {
            c.setPadding(7);
            c.setBorderWidthLeft(0);
            c.setBorderWidthRight(0);
            c.setBorderWidthTop(0);
            c.setBorderColorBottom(java.awt.Color.LIGHT_GRAY);
        }
        t.addCell(a);
        t.addCell(b);
    }

    private String fecha(Instant i) {
        return i == null ? "-" : FECHA.format(i.atZone(zona));
    }
}
