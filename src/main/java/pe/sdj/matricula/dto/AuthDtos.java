package pe.sdj.matricula.dto;

import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record SesionDto(Long id, String nombre, String rol, List<String> permisos, Long apoderadoId) {
    }

    public record TokenDto(String token, String tipo, long expiraEnSegundos, SesionDto usuario) {
    }
}
