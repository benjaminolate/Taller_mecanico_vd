package com.taller.mecanico.dto;

import java.math.BigDecimal;

/**
 * Actividad de un mecánico en el mes: órdenes en que figura como responsable,
 * trabajos que completó y la suma de sus precios.
 */
public record ResumenMecanico(
        String nombre,
        long ordenesResponsable,
        long trabajosCompletados,
        BigDecimal montoTrabajos) {
}
