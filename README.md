# Backend – Sistema de Matrícula I.E.P. Santo Domingo de Jicamarca

Spring Boot 3.3 · Java 21 · MySQL 8 · JWT. API REST bajo `/api/v1`.

Login simulado (CUBICOL stub): `POST /api/v1/auth/sso/callback {"code":"apoderado1"}`
Usuarios: `apoderado1..4`, `subdirector`, `secretaria`, `tesoreria`. Usa el token como `Authorization: Bearer ...`.

## Producción
`SPRING_PROFILES_ACTIVE=prod`, `JWT_SECRET` (obligatorio, ≥32 bytes), `DB_URL`, `DB_USER`, `DB_PASSWORD`, `FRONTEND_URL`,
y los parámetros reales de CUBICOL/MINEDU/banco (`app.integraciones.*`; hoy son puertos: `CubicolClient`, `MineduClient`).

## Diseño
- Respuesta: `{data, meta{requestId}, error{codigo,mensaje,detalles}}`; montos como string.
- Reglas RN-01…RN-10 en `service/` (reserva de 48 h, aforo = matriculados + reservas vigentes con bloqueo pesimista).
- Pagos: webhook firmado (HMAC-SHA256, cabecera `X-Signature`), guardado primero en `evento_pago` y procesado/reintentado; idempotente por (proveedor, nº operación).
- Sesión: JWT con inactividad de 15 min (token renovado en `X-Renewed-Token`), máximo 8 h, logout revoca.
- Roles/permisos sembrados en `V2__datos_referenciales.sql`.
