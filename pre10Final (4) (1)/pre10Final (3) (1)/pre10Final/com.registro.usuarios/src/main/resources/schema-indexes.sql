-- ============================================================
-- ÍNDICES DE OPTIMIZACIÓN PARA MySQL
-- Ejecutar manualmente en MySQL si Hibernate no los crea automáticamente
-- ============================================================

-- Tabla: usuarios
-- Índice en nombre (búsquedas frecuentes por nombre)
CREATE INDEX IF NOT EXISTS idx_usuario_nombre ON usuarios(nombre);

-- Índice en apellido (búsquedas frecuentes por apellido)  
CREATE INDEX IF NOT EXISTS idx_usuario_apellido ON usuarios(apellido);

-- Índice en email (ya tiene UNIQUE constraint, pero asegurar)
-- El UNIQUE constraint ya actúa como índice

-- Tabla: usuarios_roles (tabla de relación Many-to-Many)
-- Índice compuesto para JOIN frecuente
CREATE INDEX IF NOT EXISTS idx_usuarios_roles_rol ON usuarios_roles(rol_id);
CREATE INDEX IF NOT EXISTS idx_usuarios_roles_usuario ON usuarios_roles(usuarios_id);

-- Tabla: citas
-- Índice en usuario_id (buscar citas por paciente)
CREATE INDEX IF NOT EXISTS idx_cita_usuario ON citas(usuario_id);

-- Índice en medico_id (buscar citas por médico)
CREATE INDEX IF NOT EXISTS idx_cita_medico ON citas(medico_id);

-- Índice en fecha_hora (ordenar y filtrar por fecha)
CREATE INDEX IF NOT EXISTS idx_cita_fechahora ON citas(fecha_hora);

-- Índice compuesto lugar + fecha (validación de duplicados)
CREATE INDEX IF NOT EXISTS idx_cita_lugar_fecha ON citas(lugar, fecha_hora);

-- Tabla: historial_medico
-- Índice en id_usuario (buscar historiales por paciente)
CREATE INDEX IF NOT EXISTS idx_historial_usuario ON historial_medico(id_usuario);


-- ============================================================
-- VERIFICACIÓN DE CONSULTAS CON EXPLAIN
-- Ejecutar estas consultas para verificar que usan índices
-- ============================================================

-- Verificar que la consulta de pacientes por rol usa índices
-- EXPLAIN SELECT u.* FROM usuarios u 
--   JOIN usuarios_roles ur ON u.id = ur.usuarios_id 
--   WHERE ur.rol_id = 1 
--   ORDER BY u.nombre ASC 
--   LIMIT 20 OFFSET 0;

-- Verificar que la búsqueda por nombre usa índices
-- EXPLAIN SELECT u.* FROM usuarios u 
--   JOIN usuarios_roles ur ON u.id = ur.usuarios_id 
--   WHERE ur.rol_id = 1 
--     AND (u.nombre LIKE '%juan%' OR u.apellido LIKE '%juan%' OR u.email LIKE '%juan%')
--   ORDER BY u.nombre ASC 
--   LIMIT 20 OFFSET 0;

-- Verificar que la consulta de citas por médico usa índices
-- EXPLAIN SELECT * FROM citas WHERE medico_id = 1 ORDER BY fecha_hora DESC;

-- Verificar que la consulta de historial por usuario usa índices
-- EXPLAIN SELECT * FROM historial_medico WHERE id_usuario = 1;
