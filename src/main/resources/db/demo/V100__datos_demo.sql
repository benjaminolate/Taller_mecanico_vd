-- ============================================================
-- Datos de ejemplo de "Taller Demo".
-- TODOS LOS DATOS SON FICTICIOS: nombres, RUT, teléfonos, patentes
-- y precios fueron inventados solo para mostrar el sistema.
-- Flyway carga este archivo después del esquema (db/migration) cada
-- vez que arranca la app, porque la base H2 vive en memoria.
-- ============================================================

-- ── Mecánicos (3) ─────────────────────────────────────────────
INSERT INTO mecanicos (id, nombre, rut, estado_activo) VALUES
    (1, 'Mario Demo',     '11.111.111-1', TRUE),
    (2, 'Lucía Ejemplo',  '22.222.222-2', TRUE),
    (3, 'Tomás Prueba',   '33.333.333-3', TRUE);

-- ── Catálogo de trabajos con precios de ejemplo ───────────────
INSERT INTO catalogo_trabajos (id, nombre_tarea, costo_estimado_defecto, precio_incluye_iva, activo) VALUES
    (1, 'Diagnóstico general',                 15000, FALSE, TRUE),
    (2, 'Cambio de aceite y filtro',           40000, FALSE, TRUE),
    (3, 'Cambio de pastillas de freno',        35000, FALSE, TRUE),
    (4, 'Cambio de retén de cremallera',       45000, FALSE, TRUE),
    (5, 'Alineación y balanceo',               25000, FALSE, TRUE),
    (6, 'Reparación de cremallera completa',  150000, FALSE, TRUE);

-- ── Clientes (5) ──────────────────────────────────────────────
INSERT INTO clientes (id, nombre, telefono, rut, relacion_vehiculo) VALUES
    (1, 'Andrea Muestra',  '+56 9 0000 0001', '44.444.444-4', 'Dueña'),
    (2, 'Bruno Ejemplo',   '+56 9 0000 0002', '55.555.555-5', 'Dueño'),
    (3, 'Carla Ficticia',  '+56 9 0000 0003', '66.666.666-6', 'Dueña'),
    (4, 'Diego Demo',      '+56 9 0000 0004', '77.777.777-7', 'Conductor'),
    (5, 'Elena Prueba',    '+56 9 0000 0005', '88.888.888-8', 'Dueña');

-- ── Vehículos (5) ─────────────────────────────────────────────
INSERT INTO vehiculos (id, patente, marca, modelo, anio, tipo_vehiculo) VALUES
    (1, 'DEMO01', 'Toyota',    'Yaris',   2017, 'AUTOMOVIL'),
    (2, 'DEMO02', 'Chevrolet', 'Sail',    2019, 'AUTOMOVIL'),
    (3, 'DEMO03', 'Nissan',    'Navara',  2021, 'CAMIONETA'),
    (4, 'DEMO04', 'Hyundai',   'H-1',     2016, 'FURGON'),
    (5, 'DEMO05', 'Kia',       'Rio',     2020, 'AUTOMOVIL');

-- ── Órdenes de ingreso ────────────────────────────────────────
-- ORD-002 es una cremallera que llega sola, sin vehículo.
INSERT INTO ordenes_ingreso (id, fecha, tipo_ingreso, estado, cliente_id, vehiculo_id, mecanico_id, recibido_por_id) VALUES
    ('ORD-001', CURRENT_TIMESTAMP - INTERVAL '3' DAY,  'VEHICULO',   'EN_PROCESO', 1, 1,    1, 2),
    ('ORD-002', CURRENT_TIMESTAMP - INTERVAL '2' DAY,  'CREMALLERA', 'EN_PROCESO', 2, NULL, 1, 2),
    ('ORD-003', CURRENT_TIMESTAMP - INTERVAL '9' DAY,  'VEHICULO',   'PENDIENTE',  3, 3,    NULL, 1),
    ('ORD-004', CURRENT_TIMESTAMP - INTERVAL '5' DAY,  'VEHICULO',   'LISTO',      4, 4,    3, 3),
    ('ORD-005', CURRENT_TIMESTAMP - INTERVAL '20' DAY, 'VEHICULO',   'ENTREGADO',  5, 5,    2, 1),
    ('ORD-006', CURRENT_TIMESTAMP - INTERVAL '1' DAY,  'VEHICULO',   'PENDIENTE',  2, 2,    NULL, 3);

INSERT INTO estados_vehiculo (id, orden_id, rayones, golpes, obs_carroceria, nivel_combustible,
                              nivel_aceite, nivel_agua, neumaticos_ok, obs_neumaticos, obs_generales) VALUES
    (1, 'ORD-001', TRUE,  FALSE, 'Rayón leve en puerta trasera', 50, FALSE, TRUE, TRUE,  NULL, 'Cliente reporta ruido al frenar'),
    (2, 'ORD-003', FALSE, TRUE,  'Abolladura en parachoques',    25, TRUE,  TRUE, FALSE, 'Neumático delantero gastado', 'Revisar dirección'),
    (3, 'ORD-004', FALSE, FALSE, NULL,                           75, TRUE,  TRUE, TRUE,  NULL, 'Mantención de rutina'),
    (4, 'ORD-005', FALSE, FALSE, NULL,                          100, TRUE,  TRUE, TRUE,  NULL, 'Alineación y balanceo'),
    (5, 'ORD-006', TRUE,  FALSE, 'Rayón en capó',                50, TRUE,  FALSE, TRUE, NULL, 'Pierde líquido de frenos');

INSERT INTO estados_cremallera (id, orden_id, dientes_daniados, fugas_aceite, juego_excesivo,
                                bujes_gastados, marca_cremallera, es_original, obs_cremallera) VALUES
    (1, 'ORD-002', FALSE, TRUE, TRUE, FALSE, 'Genérica', FALSE, 'Fuga de aceite en el sello izquierdo');

-- ── Trabajos realizados (6) con precios de ejemplo ────────────
INSERT INTO trabajos_realizados (id, orden_id, descripcion, categoria, mecanico_id, costo_estimado,
                                 observaciones, completado, fecha_registro, fecha_completado) VALUES
    (1, 'ORD-001', 'Diagnóstico general',            'DIAGNOSTICO',   1, 15000, 'Pastillas delanteras gastadas', TRUE,
        CURRENT_TIMESTAMP - INTERVAL '3' DAY, CURRENT_TIMESTAMP - INTERVAL '2' DAY),
    (2, 'ORD-001', 'Cambio de pastillas de freno',   'CAMBIO_PIEZA',  2, 35000, NULL, FALSE,
        CURRENT_TIMESTAMP - INTERVAL '2' DAY, NULL),
    (3, 'ORD-002', 'Cambio de retén de cremallera',  'REPARACION',    1, 45000, NULL, FALSE,
        CURRENT_TIMESTAMP - INTERVAL '2' DAY, NULL),
    (4, 'ORD-004', 'Cambio de aceite y filtro',      'MANTENIMIENTO', 3, 40000, NULL, TRUE,
        CURRENT_TIMESTAMP - INTERVAL '5' DAY, CURRENT_TIMESTAMP - INTERVAL '4' DAY),
    (5, 'ORD-005', 'Alineación y balanceo',          'MANTENIMIENTO', 2, 25000, NULL, TRUE,
        CURRENT_TIMESTAMP - INTERVAL '20' DAY, CURRENT_TIMESTAMP - INTERVAL '19' DAY),
    (6, 'ORD-005', 'Revisión de suspensión',         'REVISION',      2, 20000, 'Sin observaciones', TRUE,
        CURRENT_TIMESTAMP - INTERVAL '20' DAY, CURRENT_TIMESTAMP - INTERVAL '19' DAY);

-- ── Ajustar contadores para que los registros nuevos no choquen ─
ALTER TABLE mecanicos           ALTER COLUMN id RESTART WITH 4;
ALTER TABLE catalogo_trabajos   ALTER COLUMN id RESTART WITH 7;
ALTER TABLE clientes            ALTER COLUMN id RESTART WITH 6;
ALTER TABLE vehiculos           ALTER COLUMN id RESTART WITH 6;
ALTER TABLE estados_vehiculo    ALTER COLUMN id RESTART WITH 6;
ALTER TABLE estados_cremallera  ALTER COLUMN id RESTART WITH 2;
ALTER TABLE trabajos_realizados ALTER COLUMN id RESTART WITH 7;
ALTER SEQUENCE orden_correlativo_seq RESTART WITH 7;
