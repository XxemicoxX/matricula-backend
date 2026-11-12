package pe.sdj.matricula.service;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.sdj.matricula.common.BusinessException;
import pe.sdj.matricula.config.AppProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Guarda los documentos de sustento en disco. Seguridad: solo PDF/PNG/JPG (se verifica el contenido real,
 * no solo la extensión), máximo 5 MB, y el nombre en disco lo genera el servidor (no hay path traversal).
 */
@Service
public class AlmacenamientoService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;

    public record Guardado(String nombreOriginal, String rutaRelativa, String contentType) {
    }

    private final Path raiz;

    public AlmacenamientoService(AppProperties props) {
        this.raiz = Path.of(props.getStorage().getRuta()).toAbsolutePath().normalize();
    }

    public Guardado guardar(MultipartFile archivo, String subcarpeta) {
        if (archivo == null || archivo.isEmpty()) {
            throw BusinessException.badRequest("ARCHIVO_INVALIDO", "El archivo adjunto está vacío.");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw BusinessException.badRequest("ARCHIVO_MUY_GRANDE", "El archivo supera el tamaño máximo permitido (5 MB).");
        }
        try {
            byte[] cabecera;
            try (InputStream in = archivo.getInputStream()) {
                cabecera = in.readNBytes(8);
            }
            String tipo = detectarTipo(cabecera);
            if (tipo == null) {
                throw BusinessException.badRequest("ARCHIVO_INVALIDO", "Solo se permiten archivos PDF, PNG o JPG.");
            }
            String ext = switch (tipo) {
                case "application/pdf" -> ".pdf";
                case "image/png" -> ".png";
                default -> ".jpg";
            };
            Path dir = resolver(subcarpeta);
            Files.createDirectories(dir);
            Path destino = dir.resolve(UUID.randomUUID() + ext).normalize();
            if (!destino.startsWith(raiz)) {
                throw BusinessException.badRequest("ARCHIVO_INVALIDO", "Ruta de archivo inválida.");
            }
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            return new Guardado(nombreSeguro(archivo.getOriginalFilename()), raiz.relativize(destino).toString(), tipo);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo", e);
        }
    }

    public Resource cargar(String rutaRelativa) {
        Path p = resolver(rutaRelativa);
        if (!Files.isRegularFile(p)) {
            throw BusinessException.notFound("El documento no se encuentra disponible.");
        }
        return new FileSystemResource(p);
    }

    private Path resolver(String relativa) {
        Path p = raiz.resolve(relativa).normalize();
        if (!p.startsWith(raiz)) {
            throw BusinessException.badRequest("ARCHIVO_INVALIDO", "Ruta de archivo inválida.");
        }
        return p;
    }

    private String detectarTipo(byte[] h) {
        if (h.length >= 4 && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F') return "application/pdf";
        if (h.length >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return "image/png";
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return "image/jpeg";
        return null;
    }

    private String nombreSeguro(String original) {
        String base = original == null ? "documento" : Path.of(original).getFileName().toString();
        base = base.replaceAll("[^\\p{L}\\p{N}._ -]", "_");
        return base.length() > 150 ? base.substring(base.length() - 150) : base;
    }
}
