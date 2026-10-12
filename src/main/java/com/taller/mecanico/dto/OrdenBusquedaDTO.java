package com.taller.mecanico.dto;

import com.taller.mecanico.enums.EstadoOrden;
import com.taller.mecanico.enums.VistaHistorial;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * DTO que agrupa todos los criterios de búsqueda opcionales para el listado
 * de órdenes. Se recibe como @ModelAttribute desde el formulario GET de
 * filtros avanzados (lista-ordenes.html) y viaja intacto hasta
 * OrdenIngresoSpecification, que decide qué predicados activar según
 * qué campos vengan con valor.
 *
 * Ningún campo es obligatorio: el usuario puede combinar cero, uno o
 * varios criterios simultáneamente.
 */
@Data
public class OrdenBusquedaDTO {

    /** Búsqueda parcial (LIKE, case-insensitive) sobre Vehiculo.patente */
    private String patente;

    /** Búsqueda parcial (LIKE, case-insensitive) sobre Cliente.nombre */
    private String clienteNombre;

    /** Búsqueda parcial (LIKE, case-insensitive) sobre Cliente.rut */
    private String clienteRut;

    /** Id del mecánico de algún trabajo de la orden (viene de un <select>) */
    private Long mecanicoId;

    /** Pestaña del historial (Por hacer, Pendientes, ..., Todas). */
    private VistaHistorial vista;

    /**
     * Estado exacto de la orden. Se mantiene para los enlaces antiguos y los del
     * panel de inicio (?estado=LISTO); se traduce a la pestaña equivalente.
     */
    private EstadoOrden estado;

    /** Límite inferior del rango de fechas (inclusive, 00:00:00 de ese día) */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaDesde;

    /** Límite superior del rango de fechas (inclusive, 23:59:59 de ese día) */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaHasta;

    /** Pestaña que se aplica: la indicada, la del estado o "Todas". */
    public VistaHistorial getVistaEfectiva() {
        if (vista != null) {
            return vista;
        }
        return VistaHistorial.deEstado(estado);
    }

    /** Copia de estos filtros en otra pestaña (para contar cuántas órdenes tiene cada una). */
    public OrdenBusquedaDTO enVista(VistaHistorial otraVista) {
        OrdenBusquedaDTO copia = new OrdenBusquedaDTO();
        copia.setPatente(patente);
        copia.setClienteNombre(clienteNombre);
        copia.setClienteRut(clienteRut);
        copia.setMecanicoId(mecanicoId);
        copia.setFechaDesde(fechaDesde);
        copia.setFechaHasta(fechaHasta);
        copia.setVista(otraVista);
        return copia;
    }

    // ── Paginación ────────────────────────────────────────────────────────
    @PositiveOrZero
    private int page = 0;

    @PositiveOrZero
    private int size = 10;
}
