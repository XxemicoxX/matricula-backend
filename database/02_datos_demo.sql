-- =====================================================================
-- Script 2 de 2 (OPCIONAL): datos de prueba. Ejecutar solo si quieres probar.
-- Los DNI coinciden con los usuarios SSO simulados de CUBICOL/MINEDU.
-- Requiere haber ejecutado antes 01_esquema_y_datos_base.sql
-- =====================================================================
USE matricula_sdj;

INSERT INTO apoderado (dni, nombres, apellidos, telefono, direccion, correo) VALUES
 ('43876532','Lucía','Mendoza Rojas','987654321','Jicamarca, Huarochirí','lucia.mendoza@example.com'),
 ('40123456','Carlos','Quispe Huamán','987654322','Jicamarca, Huarochirí','carlos.quispe@example.com'),
 ('41234567','Rosa','Flores Paredes','987654323','Jicamarca, Huarochirí','rosa.flores@example.com'),
 ('41000004','Pedro','Salas Vega','987654324','Jicamarca, Huarochirí','pedro.salas@example.com');

INSERT INTO estudiante (id_apoderado, id_grado_destino, dni, nombres, apellidos, fecha_nacimiento, genero,
                        correo, telefono, direccion, condicion)
SELECT a.id_apoderado, g.id_grado, x.dni, x.nombres, x.apellidos, x.nac, x.genero,
       a.correo, a.telefono, a.direccion, x.condicion
FROM (
  SELECT '43876532' ap, '1er grado' gr, '76543218' dni, 'Valentina' nombres, 'Mendoza Rojas' apellidos, DATE '2020-03-14' nac, 'F' genero, 'REGULAR' condicion
  UNION ALL SELECT '40123456','2do grado','76543299','Mateo','Quispe Soto',DATE '2019-07-02','M','REGULAR'          -- con deuda (simulada en CUBICOL)
  UNION ALL SELECT '41234567','2do grado','76500011','Sebastián','Huamaní Flores',DATE '2019-11-21','M','NO_REGULAR'
  UNION ALL SELECT '41000004','1er grado','99000001','Camila','Salas Ortega',DATE '2020-01-09','F','REGULAR'       -- MINEDU observado (simulado)
) x
JOIN apoderado a ON a.dni = x.ap
JOIN grado g ON g.nombre = x.gr AND g.nivel = 'Primaria';

INSERT INTO seccion (id_grado, nombre, aula, aforo_maximo, estado)
SELECT g.id_grado, x.nombre, CONCAT('Aula ', LEFT(g.nombre,1), x.nombre), x.aforo, 'ABIERTA'
FROM (
  SELECT '1er grado' gr, 'A' nombre, 30 aforo
  UNION ALL SELECT '1er grado','B',30
  UNION ALL SELECT '1er grado','C',1      -- aforo de 1 para probar RN-03
  UNION ALL SELECT '2do grado','A',30
) x
JOIN grado g ON g.nombre = x.gr AND g.nivel = 'Primaria';
