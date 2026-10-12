-- ============================================================
-- V4: mecánico responsable de cada orden. Como el usuario "empleado"
-- es compartido, aquí queda registrado quién hizo el trabajo.
-- ============================================================
ALTER TABLE ordenes_ingreso ADD COLUMN mecanico_id BIGINT REFERENCES mecanicos (id);
CREATE INDEX idx_ordenes_mecanico ON ordenes_ingreso (mecanico_id);
