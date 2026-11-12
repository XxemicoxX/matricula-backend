package pe.sdj.matricula.integration;

import java.time.LocalDate;

/** MINEDU simulado: los DNI que empiezan con "99" vuelven "observados". Solo desarrollo/pruebas. */
public class StubMineduClient implements MineduClient {

    @Override
    public ResultadoValidacion validar(String dni, String nombres, String apellidos, LocalDate fechaNacimiento) {
        if (dni != null && dni.startsWith("99")) {
            return new ResultadoValidacion(false, "Los datos del estudiante no coinciden con el registro oficial (SIAGIE).");
        }
        return new ResultadoValidacion(true, "Datos conformes con el registro oficial.");
    }
}
