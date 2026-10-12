-- ============================================================
-- V6: separa quién recibe el vehículo de quién realiza el trabajo.
--   recibido_por_id: mecánico que recibió al cliente (obligatorio al crear la orden).
--   mecanico_id:     mecánico que realiza el trabajo; ahora se asigna después,
--                    desde el historial o el detalle de la orden.
-- En las órdenes existentes, quien recibió se toma del mecánico responsable.
-- ============================================================
ALTER TABLE ordenes_ingreso ADD COLUMN recibido_por_id BIGINT REFERENCES mecanicos (id);
UPDATE ordenes_ingreso SET recibido_por_id = mecanico_id WHERE recibido_por_id IS NULL;
CREATE INDEX idx_ordenes_recibido_por ON ordenes_ingreso (recibido_por_id);
