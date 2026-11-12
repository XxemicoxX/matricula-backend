-- =====================================================================
-- Sistema de Matrícula I.E.P. Santo Domingo de Jicamarca
-- Script 1 de 2: esquema + datos base (roles, permisos, grados)
-- Para MySQL 8.x. Ejecutar completo en MySQL Workbench (File > Open SQL Script > Ejecutar todo).
-- Las fechas se guardan en UTC.
-- =====================================================================
CREATE DATABASE IF NOT EXISTS matricula_sdj CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE matricula_sdj;


CREATE TABLE rol (
    id_rol      BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(50)  NOT NULL,
    descripcion VARCHAR(200),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre)
);

CREATE TABLE permiso (
    id_permiso  BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(200),
    CONSTRAINT uq_permiso_nombre UNIQUE (nombre)
);

CREATE TABLE rol_permiso (
    id_rol     BIGINT NOT NULL,
    id_permiso BIGINT NOT NULL,
    PRIMARY KEY (id_rol, id_permiso),
    CONSTRAINT fk_rp_rol     FOREIGN KEY (id_rol)     REFERENCES rol (id_rol),
    CONSTRAINT fk_rp_permiso FOREIGN KEY (id_permiso) REFERENCES permiso (id_permiso)
);

CREATE TABLE apoderado (
    id_apoderado BIGINT AUTO_INCREMENT PRIMARY KEY,
    dni          VARCHAR(20)  NOT NULL,
    nombres      VARCHAR(100) NOT NULL,
    apellidos    VARCHAR(100) NOT NULL,
    telefono     VARCHAR(20),
    direccion    VARCHAR(200),
    correo       VARCHAR(150),
    CONSTRAINT uq_apoderado_dni UNIQUE (dni)
);

-- Sin contraseñas: la identidad la provee CUBICOL (SSO). identificador_sso es el id externo.
CREATE TABLE usuario (
    id_usuario       BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_rol           BIGINT       NOT NULL,
    id_apoderado     BIGINT,
    identificador_sso VARCHAR(100) NOT NULL,
    nombres          VARCHAR(100) NOT NULL,
    apellidos        VARCHAR(100) NOT NULL,
    correo           VARCHAR(150),
    estado           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT uq_usuario_sso UNIQUE (identificador_sso),
    CONSTRAINT fk_usuario_rol       FOREIGN KEY (id_rol)       REFERENCES rol (id_rol),
    CONSTRAINT fk_usuario_apoderado FOREIGN KEY (id_apoderado) REFERENCES apoderado (id_apoderado)
);

CREATE TABLE grado (
    id_grado        BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(50)   NOT NULL,
    nivel           VARCHAR(50)   NOT NULL,
    monto_matricula DECIMAL(10,2) NOT NULL,
    CONSTRAINT uq_grado UNIQUE (nombre, nivel)
);

CREATE TABLE seccion (
    id_seccion          BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_grado            BIGINT      NOT NULL,
    nombre              VARCHAR(10) NOT NULL,
    aula                VARCHAR(50),
    docente_responsable VARCHAR(150),
    aforo_maximo        INT         NOT NULL,
    estado              VARCHAR(20) NOT NULL DEFAULT 'ABIERTA',
    CONSTRAINT uq_seccion UNIQUE (id_grado, nombre),
    CONSTRAINT ck_seccion_aforo CHECK (aforo_maximo > 0),
    CONSTRAINT fk_seccion_grado FOREIGN KEY (id_grado) REFERENCES grado (id_grado)
);

-- condicion: REGULAR | NO_REGULAR (DA-01 "¿Matrícula regular?"); excepcion_aprobada se activa
-- cuando Secretaría aprueba un caso excepcional (RN-10).
CREATE TABLE estudiante (
    id_estudiante      BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_apoderado       BIGINT       NOT NULL,
    id_grado_destino   BIGINT,
    dni                VARCHAR(20)  NOT NULL,
    nombres            VARCHAR(100) NOT NULL,
    apellidos          VARCHAR(100) NOT NULL,
    fecha_nacimiento   DATE,
    genero             VARCHAR(20),
    correo             VARCHAR(150),
    telefono           VARCHAR(20),
    direccion          VARCHAR(200),
    condicion          VARCHAR(20)  NOT NULL DEFAULT 'REGULAR',
    excepcion_aprobada BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_estudiante_dni UNIQUE (dni),
    CONSTRAINT fk_estudiante_apoderado FOREIGN KEY (id_apoderado)     REFERENCES apoderado (id_apoderado),
    CONSTRAINT fk_estudiante_grado     FOREIGN KEY (id_grado_destino) REFERENCES grado (id_grado)
);

CREATE TABLE matricula (
    id_matricula     BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante    BIGINT      NOT NULL,
    id_seccion       BIGINT      NOT NULL,
    periodo_academico VARCHAR(20) NOT NULL,
    fecha_matricula  DATETIME,
    estado           VARCHAR(30) NOT NULL,
    fecha_reserva    DATETIME    NOT NULL,
    fecha_expiracion DATETIME    NOT NULL,
    CONSTRAINT fk_matricula_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante (id_estudiante),
    CONSTRAINT fk_matricula_seccion    FOREIGN KEY (id_seccion)    REFERENCES seccion (id_seccion)
);
CREATE INDEX ix_matricula_seccion_estado ON matricula (id_seccion, estado);
CREATE INDEX ix_matricula_estudiante     ON matricula (id_estudiante, periodo_academico);
CREATE INDEX ix_matricula_expiracion     ON matricula (estado, fecha_expiracion);

CREATE TABLE codigo_pago (
    id_codigo_pago   BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_matricula     BIGINT        NOT NULL,
    codigo           VARCHAR(50)   NOT NULL,
    monto            DECIMAL(10,2) NOT NULL,
    fecha_generacion DATETIME      NOT NULL,
    fecha_expiracion DATETIME      NOT NULL,
    estado           VARCHAR(30)   NOT NULL,
    CONSTRAINT uq_codigo_pago_codigo   UNIQUE (codigo),
    CONSTRAINT uq_codigo_pago_matricula UNIQUE (id_matricula),
    CONSTRAINT fk_codigo_matricula FOREIGN KEY (id_matricula) REFERENCES matricula (id_matricula)
);

-- Un mismo (proveedor, numero_operacion) nunca se registra dos veces: idempotencia de pagos.
CREATE TABLE pago (
    id_pago               BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_codigo_pago        BIGINT        NOT NULL,
    id_usuario            BIGINT,
    proveedor             VARCHAR(50)   NOT NULL,
    numero_operacion      VARCHAR(100)  NOT NULL,
    monto                 DECIMAL(10,2) NOT NULL,
    fecha_pago            DATETIME      NOT NULL,
    metodo_pago           VARCHAR(50),
    tipo_pago             VARCHAR(20)   NOT NULL,
    estado                VARCHAR(30)   NOT NULL,
    entidad_financiera    VARCHAR(100),
    referencia_comprobante VARCHAR(150),
    fecha_registro        DATETIME      NOT NULL,
    CONSTRAINT uq_pago_operacion UNIQUE (proveedor, numero_operacion),
    CONSTRAINT fk_pago_codigo  FOREIGN KEY (id_codigo_pago) REFERENCES codigo_pago (id_codigo_pago),
    CONSTRAINT fk_pago_usuario FOREIGN KEY (id_usuario)     REFERENCES usuario (id_usuario)
);

-- Bandeja durable de confirmaciones del banco (RNF "Confiabilidad de pagos": 0 % de pérdida).
CREATE TABLE evento_pago (
    id_evento             BIGINT AUTO_INCREMENT PRIMARY KEY,
    proveedor             VARCHAR(50)  NOT NULL,
    idempotencia          VARCHAR(100) NOT NULL,
    payload               TEXT         NOT NULL,
    estado                VARCHAR(20)  NOT NULL,
    intentos              INT          NOT NULL DEFAULT 0,
    detalle               VARCHAR(500),
    fecha_recepcion       DATETIME     NOT NULL,
    fecha_procesamiento   DATETIME,
    CONSTRAINT uq_evento_idempotencia UNIQUE (proveedor, idempotencia)
);
CREATE INDEX ix_evento_estado ON evento_pago (estado, intentos);

CREATE TABLE deuda (
    id_deuda       BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante  BIGINT        NOT NULL,
    periodo        VARCHAR(20)   NOT NULL,
    concepto       VARCHAR(100)  NOT NULL,
    monto          DECIMAL(10,2) NOT NULL,
    estado         VARCHAR(30)   NOT NULL,
    fecha_consulta DATETIME      NOT NULL,
    CONSTRAINT uq_deuda UNIQUE (id_estudiante, periodo, concepto),
    CONSTRAINT fk_deuda_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante (id_estudiante)
);

-- tipo_solicitud: CAMBIO_DATOS | CASO_EXCEPCIONAL (ambos siguen el mismo flujo, Flujo 2 del documento)
CREATE TABLE solicitud_modificacion (
    id_solicitud           BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante          BIGINT      NOT NULL,
    id_usuario_solicitante BIGINT      NOT NULL,
    id_usuario_revisor     BIGINT,
    tipo_solicitud         VARCHAR(50) NOT NULL,
    motivo                 VARCHAR(50),
    descripcion            TEXT,
    datos_solicitados      TEXT,
    estado                 VARCHAR(30) NOT NULL,
    fecha_solicitud        DATETIME    NOT NULL,
    fecha_respuesta        DATETIME,
    observacion            TEXT,
    CONSTRAINT fk_sol_estudiante  FOREIGN KEY (id_estudiante)          REFERENCES estudiante (id_estudiante),
    CONSTRAINT fk_sol_solicitante FOREIGN KEY (id_usuario_solicitante) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_sol_revisor     FOREIGN KEY (id_usuario_revisor)     REFERENCES usuario (id_usuario)
);
CREATE INDEX ix_solicitud_estado ON solicitud_modificacion (estado, tipo_solicitud);

CREATE TABLE documento (
    id_documento   BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_solicitud   BIGINT       NOT NULL,
    nombre_archivo VARCHAR(200) NOT NULL,
    ruta_archivo   VARCHAR(300) NOT NULL,
    tipo_documento VARCHAR(100) NOT NULL,
    fecha_subida   DATETIME     NOT NULL,
    CONSTRAINT fk_documento_solicitud FOREIGN KEY (id_solicitud) REFERENCES solicitud_modificacion (id_solicitud)
);

CREATE TABLE validacion_minedu (
    id_validacion     BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante     BIGINT      NOT NULL,
    periodo_academico VARCHAR(20) NOT NULL,
    fecha_validacion  DATETIME    NOT NULL,
    resultado         VARCHAR(30) NOT NULL,
    observacion       TEXT,
    CONSTRAINT fk_minedu_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiante (id_estudiante)
);
CREATE INDEX ix_minedu_estudiante ON validacion_minedu (id_estudiante, periodo_academico);

-- Una notificación va a un apoderado (id_apoderado) o a un rol del personal (rol_destino).
CREATE TABLE notificacion (
    id_notificacion BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_matricula    BIGINT,
    id_apoderado    BIGINT,
    rol_destino     VARCHAR(50),
    tipo            VARCHAR(50)  NOT NULL,
    canal           VARCHAR(20)  NOT NULL,
    destinatario    VARCHAR(150),
    mensaje         TEXT         NOT NULL,
    fecha_envio     DATETIME     NOT NULL,
    estado          VARCHAR(30)  NOT NULL,
    leida           BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_notif_matricula FOREIGN KEY (id_matricula) REFERENCES matricula (id_matricula),
    CONSTRAINT fk_notif_apoderado FOREIGN KEY (id_apoderado) REFERENCES apoderado (id_apoderado)
);
CREATE INDEX ix_notif_apoderado ON notificacion (id_apoderado, fecha_envio);
CREATE INDEX ix_notif_rol       ON notificacion (rol_destino, fecha_envio);

CREATE TABLE auditoria (
    id_auditoria BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario   BIGINT,
    accion       VARCHAR(100) NOT NULL,
    fecha_hora   DATETIME     NOT NULL,
    direccion_ip VARCHAR(45),
    descripcion  VARCHAR(255),
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);
CREATE INDEX ix_auditoria_fecha ON auditoria (fecha_hora);

-- Sesiones cerradas (logout) para invalidar el JWT antes de su expiración.
CREATE TABLE sesion_revocada (
    jti    VARCHAR(64) PRIMARY KEY,
    expira DATETIME    NOT NULL
);

-- ---------------- Datos base ----------------

INSERT INTO rol (nombre, descripcion) VALUES
    ('APODERADO',  'Padre, madre o tutor legal que realiza la matrícula'),
    ('SUBDIRECTOR','Gestión de secciones, aforo, dashboard y reportes'),
    ('SECRETARIA', 'Solicitudes de cambio de datos, casos excepcionales y contingencias'),
    ('TESORERIA',  'Conciliación de pagos y registro manual de operaciones');

INSERT INTO permiso (nombre, descripcion) VALUES
    ('CONSULTAR_ESTUDIANTE',    'Consultar datos del estudiante'),
    ('ACTUALIZAR_CONTACTO',     'Actualizar datos de contacto (teléfono, dirección, correo)'),
    ('SOLICITAR_CAMBIO_DATOS',  'Solicitar cambio de datos fijos con sustento documentario'),
    ('SOLICITAR_EXCEPCION',     'Solicitar atención de caso excepcional'),
    ('GESTIONAR_SOLICITUDES',   'Revisar y resolver solicitudes de cambio de datos'),
    ('GESTIONAR_EXCEPCIONES',   'Revisar y resolver casos excepcionales'),
    ('CONSULTAR_DEUDAS',        'Consultar deudas del estudiante'),
    ('CONSULTAR_SECCIONES',     'Consultar secciones y vacantes'),
    ('GESTIONAR_SECCIONES',     'Crear/cerrar secciones y configurar aforo'),
    ('GENERAR_CODIGO_PAGO',     'Seleccionar sección, reservar y generar código de pago'),
    ('REGISTRAR_PAGO_MANUAL',   'Registrar pagos manuales de contingencia'),
    ('CONSULTAR_DASHBOARD',     'Consultar dashboard de aforo'),
    ('GENERAR_REPORTES',        'Generar y exportar reportes de matrícula'),
    ('CONSULTAR_AUDITORIA',     'Consultar registro de auditoría');

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso FROM rol r, permiso p
WHERE r.nombre = 'APODERADO' AND p.nombre IN
    ('CONSULTAR_ESTUDIANTE','ACTUALIZAR_CONTACTO','SOLICITAR_CAMBIO_DATOS','SOLICITAR_EXCEPCION',
     'CONSULTAR_DEUDAS','CONSULTAR_SECCIONES','GENERAR_CODIGO_PAGO');

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso FROM rol r, permiso p
WHERE r.nombre = 'SUBDIRECTOR' AND p.nombre IN
    ('CONSULTAR_ESTUDIANTE','CONSULTAR_DEUDAS','CONSULTAR_SECCIONES','GESTIONAR_SECCIONES',
     'CONSULTAR_DASHBOARD','GENERAR_REPORTES','CONSULTAR_AUDITORIA');

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso FROM rol r, permiso p
WHERE r.nombre = 'SECRETARIA' AND p.nombre IN
    ('CONSULTAR_ESTUDIANTE','ACTUALIZAR_CONTACTO','CONSULTAR_DEUDAS','CONSULTAR_SECCIONES',
     'GESTIONAR_SOLICITUDES','GESTIONAR_EXCEPCIONES','REGISTRAR_PAGO_MANUAL',
     'CONSULTAR_DASHBOARD','GENERAR_REPORTES');

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso FROM rol r, permiso p
WHERE r.nombre = 'TESORERIA' AND p.nombre IN
    ('CONSULTAR_ESTUDIANTE','CONSULTAR_DEUDAS','REGISTRAR_PAGO_MANUAL','CONSULTAR_DASHBOARD');

-- Grados (monto de matrícula editable por grado; S/ 650.00 según el prototipo de interfaz).
INSERT INTO grado (nombre, nivel, monto_matricula) VALUES
    ('1er grado', 'Primaria',   650.00),
    ('2do grado', 'Primaria',   650.00),
    ('3er grado', 'Primaria',   650.00),
    ('4to grado', 'Primaria',   650.00),
    ('5to grado', 'Primaria',   650.00),
    ('6to grado', 'Primaria',   650.00),
    ('1er grado', 'Secundaria', 650.00),
    ('2do grado', 'Secundaria', 650.00),
    ('3er grado', 'Secundaria', 650.00),
    ('4to grado', 'Secundaria', 650.00),
    ('5to grado', 'Secundaria', 650.00);
