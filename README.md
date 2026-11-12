# Backend – Sistema de Matrícula I.E.P. Santo Domingo de Jicamarca

Spring Boot 3.3 · Java 21 · MySQL 8 (tu servidor) · JWT. API REST bajo `/api/v1` (Swagger en `/swagger-ui.html`).

## Base de datos (MySQL 8 / Workbench)
1. En MySQL Workbench abre y ejecuta `database/01_esquema_y_datos_base.sql` (crea la BD `matricula_sdj`, tablas, roles, permisos y grados).
2. Opcional, para probar: ejecuta `database/02_datos_demo.sql` (apoderados, estudiantes y secciones de ejemplo).
3. La aplicación **no crea ni modifica tablas**; solo se conecta a tu MySQL. Si cambias entidades, actualiza el script.

## Ejecutar
```bash
export DB_URL="jdbc:mysql://localhost:3306/matricula_sdj?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USER=root DB_PASSWORD=tu_clave
mvn spring-boot:run          # perfil dev: tu MySQL + CUBICOL/MINEDU/banco simulados
mvn test                     # también usa tu MySQL (requiere el script 02 cargado)
```
Login simulado (CUBICOL stub): `POST /api/v1/auth/sso/callback {"code":"apoderado1"}`
Usuarios: `apoderado1..4`, `subdirector`, `secretaria`, `tesoreria`. Usa el token como `Authorization: Bearer ...`.

## Producción
`SPRING_PROFILES_ACTIVE=prod`, `JWT_SECRET` (obligatorio, ≥32 bytes), `DB_URL`, `DB_USER`, `DB_PASSWORD`, `FRONTEND_URL`,
y los parámetros reales de CUBICOL/MINEDU/banco (`app.integraciones.*`; hoy son puertos: `CubicolClient`, `MineduClient`).
Los stubs están prohibidos en prod.

## Diseño
- Respuesta: `{data, meta{requestId}, error{codigo,mensaje,detalles}}`; montos como string.
- Reglas RN-01…RN-10 en `service/` (reserva de 48 h, aforo = matriculados + reservas vigentes con bloqueo pesimista).
- Pagos: webhook firmado (HMAC-SHA256, cabecera `X-Signature`), guardado primero en `evento_pago` y procesado/reintentado; idempotente por (proveedor, nº operación).
- Sesión: JWT con inactividad de 15 min (token renovado en `X-Renewed-Token`), máximo 8 h, logout revoca.
- Roles/permisos sembrados en `V2__datos_referenciales.sql`.
