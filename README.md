# Taller Mecánico · Gestión de órdenes de trabajo

Aplicación web para que un taller mecánico registre el ingreso de vehículos y cremalleras, los trabajos realizados, los mecánicos responsables y el estado de cada orden hasta su entrega.

> **Versión demo.** Este repositorio es una versión de demostración: todos los datos (taller, clientes, vehículos, mecánicos, RUT, teléfonos, patentes y precios) son **ficticios** y se cargan en una base H2 en memoria que se borra al cerrar la aplicación.

---

## Capturas de pantalla

<!-- Reemplazar cada línea por la imagen correspondiente, por ejemplo: ![Panel de inicio](docs/capturas/panel.png) -->

| Pantalla | Captura |
| --- | --- |
| Panel de inicio | _[captura pendiente: docs/capturas/panel.png]_ |
| Historial de órdenes con filtros | _[captura pendiente: docs/capturas/historial.png]_ |
| Nueva orden de ingreso | _[captura pendiente: docs/capturas/nueva-orden.png]_ |
| Detalle de la orden y trabajos | _[captura pendiente: docs/capturas/detalle-orden.png]_ |
| Orden en PDF | _[captura pendiente: docs/capturas/orden-pdf.png]_ |

---

## Problema que resuelve

En muchos talleres pequeños las órdenes se anotan en papel o en planillas sueltas: cuesta saber qué vehículos están pendientes, quién está trabajando en cada uno, cuánto se ha cobrado o en qué estado llegó el auto. Esta aplicación centraliza ese registro:

- Cada vehículo (o cremallera de dirección que llega sola) ingresa con una **orden numerada** (`ORD-001`, `ORD-002`, …) que guarda el estado en que llegó.
- Los trabajos se agregan a la orden con su mecánico y precio, y la orden avanza por estados (**Pendiente → En proceso → Listo → Entregado**).
- El historial permite encontrar cualquier orden por patente, cliente, RUT, mecánico, estado o rango de fechas, y generar la orden en PDF.

---

## Funcionalidades

**Órdenes de trabajo**
- Ingreso de **vehículos** (checklist de carrocería, combustible, aceite, agua y neumáticos) o de **cremalleras** (daños, fugas, juego, bujes, marca).
- Correlativo automático `ORD-NNN` y flujo de estados con transiciones validadas.
- Registro de quién **recibió** el vehículo y de quién **realiza** el trabajo.
- Edición de la orden y detalle con total de trabajos.

**Clientes y vehículos**
- Ficha de clientes con búsqueda por nombre, teléfono o RUT, y historial de órdenes de cada cliente.
- Validación y formato de **RUT** chileno (dígito verificador) y normalización de **patentes** (`demo-01` → `DEMO01`).
- Si la patente ya existe, la orden reutiliza el vehículo registrado.

**Mecánicos y catálogo de trabajos**
- Mantención de mecánicos (activar/desactivar sin perder su historial).
- **Catálogo de trabajos** con precio sugerido (neto o con IVA) que se usa al agregar trabajos a una orden.

**Búsqueda, filtros y paginación**
- **Búsqueda multi-criterio** con **Spring Data JPA Specification / Criteria API** (patente, nombre de cliente, RUT, mecánico, estado, fecha desde/hasta), combinando solo los filtros que vienen informados.
- **Paginación** del historial (`Pageable`) y pestañas por estado (Por hacer, Pendientes, En proceso, Listas, Entregadas, Todas) con su cantidad.
- **Filtros en Bootstrap 5** e indicador de días en el taller por colores.

**Autocompletado AJAX**
- Al crear una orden, `fetch()` consulta endpoints JSON para completar los datos del **cliente** seleccionado y del **vehículo** a partir de su patente.

**Reportes en PDF**
- La orden se descarga en **PDF** generado con **Flying Saucer** a partir de una plantilla Thymeleaf (XHTML), incluyendo las firmas registradas.

**Otras funciones implementadas**
- **Login con roles** (Spring Security): `JEFE` (administra cuentas, mecánicos, catálogo y estados) y `EMPLEADO`.
- **Panel de inicio** con órdenes listas para entregar, órdenes atrasadas y, para el jefe, actividad del mes por mecánico.
- **Firma del cliente** en pantalla táctil (recepción y retiro), que aparece en el detalle y en el PDF.
- **Aviso por WhatsApp** al cliente cuando la orden está lista (enlace con mensaje prellenado).
- **Auditoría**: cada orden y trabajo registra quién lo creó y modificó.
- **Migraciones versionadas** con Flyway y **89 pruebas automatizadas** (JUnit 5 + MockMvc) que corren en GitHub Actions.

---

## Tecnologías

| Capa | Tecnología |
| --- | --- |
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.3 (Spring MVC, Spring Data JPA, Spring Security, Bean Validation) |
| Vistas | Thymeleaf + Thymeleaf Layout Dialect, Bootstrap 5, JavaScript (fetch) |
| Persistencia | Hibernate / JPA, Specification (Criteria API), Flyway |
| Base de datos (demo) | H2 en memoria (modo compatible con PostgreSQL) |
| PDF | Flying Saucer |
| Utilidades | Lombok |
| Pruebas | JUnit 5, Spring Boot Test, MockMvc, Spring Security Test |
| Build / CI | Maven (wrapper incluido), GitHub Actions |

---

## Cómo ejecutarlo

**Requisitos:** Java 17 o superior. No hace falta instalar Maven ni una base de datos.

1. Clonar el repositorio:

   ```bash
   git clone https://github.com/benjaminolate/Taller_mecanico_vd.git
   cd Taller_mecanico_vd
   ```

2. Arrancar la aplicación:

   ```bash
   # Linux / macOS
   ./mvnw spring-boot:run

   # Windows
   mvnw.cmd spring-boot:run
   ```

3. Abrir <http://localhost:8080> e ingresar con una de las cuentas de la demo:

   | Usuario | Contraseña | Rol |
   | --- | --- | --- |
   | `jefe` | `jefedemo` | Jefe (acceso completo) |
   | `empleado` | `empleadodemo` | Empleado |

4. (Opcional) Correr las pruebas:

   ```bash
   ./mvnw verify
   ```

Al arrancar, Flyway crea el esquema y carga los datos ficticios de **Taller Demo**: 5 clientes, 5 vehículos, 3 mecánicos, 6 órdenes y 6 trabajos con precios de ejemplo (`src/main/resources/db/demo/V100__datos_demo.sql`).

**Variables de entorno opcionales**

| Variable | Uso |
| --- | --- |
| `PORT` | Puerto HTTP (por defecto `8080`) |
| `ADMIN_PASSWORD` / `EMPLEADO_PASSWORD` | Contraseñas iniciales de las cuentas de la demo |
| `PASSWORD_RESPALDO_HASH` | Hash BCrypt de una contraseña de respaldo (desactivada si no se define) |

---

## Estructura de carpetas

```text
src/main/java/com/taller/mecanico/
├── config/          Seguridad, autenticación, auditoría y cuentas iniciales
├── controller/      Rutas web y endpoints JSON del autocompletado
├── dto/             Datos de formularios y criterios de búsqueda
├── entity/          Entidades JPA
├── enums/           Estados, tipos y categorías
├── exception/       Manejo global de errores
├── repository/      Repositorios Spring Data
├── service/         Lógica de negocio y generación de PDF
├── specification/   Filtros dinámicos de la búsqueda de órdenes (Criteria API)
└── util/            Validación de RUT, patentes, teléfonos y precios
src/main/resources/
├── db/migration/    Migraciones Flyway (esquema)
├── db/demo/         Datos ficticios de la demo
├── static/vendor/   Bootstrap 5 incluido localmente
└── templates/       Vistas Thymeleaf (incluye la plantilla del PDF)
src/test/java/       Pruebas de servicios, pantallas y seguridad
```

---

## Estado y próximos pasos

**Estado:** funcional. Esta versión demo usa una base H2 en memoria y datos inventados.

**Próximos pasos**
- Emisión de boleta y factura electrónica.
- Registro de pagos al momento de la entrega.
- Definir garantías y condiciones del servicio impresas en la orden.

---

## Autor

**Benjamin Olate** · [github.com/benjaminolate](https://github.com/benjaminolate)

El proyecto se originó en conjunto con **[NOMBRE DEL COMPAÑERO]** ([github.com/Lieztz3r](https://github.com/Lieztz3r)).
