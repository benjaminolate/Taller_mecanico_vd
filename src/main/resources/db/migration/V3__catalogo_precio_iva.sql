-- ============================================================
-- V3: el precio del catálogo puede ingresarse neto o con IVA,
-- y las tareas que ya no se ofrecen se desactivan en vez de borrarse.
-- ============================================================
ALTER TABLE catalogo_trabajos ADD COLUMN precio_incluye_iva BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE catalogo_trabajos ADD COLUMN activo             BOOLEAN NOT NULL DEFAULT TRUE;
