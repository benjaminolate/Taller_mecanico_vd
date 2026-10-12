package com.taller.mecanico.repository;

import com.taller.mecanico.entity.TrabajoRealizado;
import com.taller.mecanico.enums.CategoriaTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TrabajoRealizadoRepository extends JpaRepository<TrabajoRealizado, Long> {

    /**
     * Todos los trabajos de una orden específica, ordenados por fecha de registro.
     * Usado para mostrar el listado de trabajos dentro del detalle de una orden.
     */
    List<TrabajoRealizado> findByOrdenIdOrderByFechaRegistroAsc(String ordenId);

    /**
     * Solo los trabajos pendientes de una orden (completado = false).
     */
    List<TrabajoRealizado> findByOrdenIdAndCompletadoFalse(String ordenId);

    /**
     * Trabajos completados de una orden.
     */
    List<TrabajoRealizado> findByOrdenIdAndCompletadoTrue(String ordenId);

    /**
     * Filtra trabajos por categoría dentro de una orden.
     */
    List<TrabajoRealizado> findByOrdenIdAndCategoria(String ordenId, CategoriaTrabajo categoria);

    /**
     * Suma el costo total estimado de todos los trabajos de una orden.
     * Retorna null si no hay trabajos o todos tienen costo null.
     */
    @Query("SELECT SUM(t.costoEstimado) FROM TrabajoRealizado t WHERE t.orden.id = :ordenId")
    BigDecimal sumCostoEstimadoByOrdenId(@Param("ordenId") String ordenId);

    /**
     * Cuenta cuántos trabajos tiene una orden (útil para el listado de órdenes).
     */
    long countByOrdenId(String ordenId);


    /** Trabajos completados por mecánico desde una fecha: [id mecánico, cantidad, suma de costos]. */
    @Query("SELECT t.mecanico.id, COUNT(t), COALESCE(SUM(t.costoEstimado), 0) FROM TrabajoRealizado t "
         + "WHERE t.mecanico IS NOT NULL AND t.completado = true AND t.fechaCompletado >= :desde "
         + "GROUP BY t.mecanico.id")
    List<Object[]> resumenCompletadosPorMecanicoDesde(@Param("desde") java.time.LocalDateTime desde);
}
