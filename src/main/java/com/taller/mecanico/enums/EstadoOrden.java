package com.taller.mecanico.enums;

import java.util.Set;

/**
 * Estados de una orden y los cambios permitidos entre ellos:
 *
 * <pre>
 * PENDIENTE  → EN_PROCESO
 * EN_PROCESO → PENDIENTE, LISTO
 * LISTO      → EN_PROCESO (se reabre), ENTREGADO
 * ENTREGADO  → (final, no admite cambios)
 * </pre>
 */
public enum EstadoOrden {
    PENDIENTE("Pendiente"),
    EN_PROCESO("En proceso"),
    LISTO("Listo"),
    ENTREGADO("Entregado");

    private final String etiqueta;

    EstadoOrden(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Nombre para mostrar en pantalla. */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** Estados a los que se puede pasar desde este. */
    public Set<EstadoOrden> getSiguientesPermitidos() {
        return switch (this) {
            case PENDIENTE  -> Set.of(EN_PROCESO);
            case EN_PROCESO -> Set.of(PENDIENTE, LISTO);
            case LISTO      -> Set.of(EN_PROCESO, ENTREGADO);
            case ENTREGADO  -> Set.of();
        };
    }

    public boolean puedeCambiarA(EstadoOrden destino) {
        return getSiguientesPermitidos().contains(destino);
    }

    /** Una orden entregada ya no admite trabajos nuevos ni cambios. */
    public boolean esFinal() {
        return this == ENTREGADO;
    }
}
