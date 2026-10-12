package com.taller.mecanico.enums;

import java.util.Set;

/**
 * Pestañas del historial de órdenes. Cada una muestra un grupo de estados.
 * Las pestañas de trabajo pendiente se ordenan de la más antigua a la más
 * nueva (lo más urgente arriba); las de historial, de la más nueva a la más antigua.
 */
public enum VistaHistorial {
    POR_HACER ("Por hacer",            Set.of(EstadoOrden.PENDIENTE, EstadoOrden.EN_PROCESO), true),
    PENDIENTE ("Pendientes",           Set.of(EstadoOrden.PENDIENTE),  true),
    EN_PROCESO("En proceso",           Set.of(EstadoOrden.EN_PROCESO), true),
    LISTO     ("Listas para entregar", Set.of(EstadoOrden.LISTO),      true),
    ENTREGADO ("Entregadas",           Set.of(EstadoOrden.ENTREGADO),  false),
    TODAS     ("Todas",                Set.of(),                       false);

    private final String etiqueta;
    private final Set<EstadoOrden> estados;
    private final boolean masAntiguasPrimero;

    VistaHistorial(String etiqueta, Set<EstadoOrden> estados, boolean masAntiguasPrimero) {
        this.etiqueta = etiqueta;
        this.estados = estados;
        this.masAntiguasPrimero = masAntiguasPrimero;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Estados que incluye la pestaña. Vacío = todos. */
    public Set<EstadoOrden> getEstados() {
        return estados;
    }

    public boolean isMasAntiguasPrimero() {
        return masAntiguasPrimero;
    }

    /** Pestaña que corresponde a un único estado (para los enlaces con ?estado=...). */
    public static VistaHistorial deEstado(EstadoOrden estado) {
        return estado == null ? TODAS : valueOf(estado.name());
    }
}
