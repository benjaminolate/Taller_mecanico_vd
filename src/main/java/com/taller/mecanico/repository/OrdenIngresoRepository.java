package com.taller.mecanico.repository;

import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.enums.EstadoOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Extiende JpaSpecificationExecutor para la búsqueda multi-criterio
 * (ver OrdenIngresoSpecification).
 */
@Repository
public interface OrdenIngresoRepository extends
        JpaRepository<OrdenIngreso, String>,
        JpaSpecificationExecutor<OrdenIngreso> {

    /**
     * Siguiente número del correlativo "ORD-NNN". nextval() es atómico en
     * PostgreSQL, así que dos órdenes simultáneas nunca obtienen el mismo número.
     * La secuencia se crea en db/migration/V1__esquema_inicial.sql.
     */
    @Query(value = "SELECT nextval('orden_correlativo_seq')", nativeQuery = true)
    long siguienteCorrelativo();

    /** Historial de un cliente, de la más nueva a la más antigua. */
    List<OrdenIngreso> findByClienteIdOrderByFechaDesc(Long clienteId);

    long countByClienteId(Long clienteId);

    // ── Panel de inicio ──────────────────────────────────────────────────────

    long countByEstado(EstadoOrden estado);

    long countByFechaGreaterThanEqual(LocalDateTime desde);

    /** Listas para entregar, de la más antigua a la más nueva. */
    List<OrdenIngreso> findTop20ByEstadoOrderByFechaAsc(EstadoOrden estado);

    /** En alguno de los estados e ingresadas antes de la fecha límite, de la más antigua a la más nueva. */
    List<OrdenIngreso> findTop20ByEstadoInAndFechaBeforeOrderByFechaAsc(Collection<EstadoOrden> estados, LocalDateTime limite);

    /** Órdenes sin mecánico asignado en los estados indicados, de la más antigua a la más nueva. */
    List<OrdenIngreso> findTop20ByMecanicoIsNullAndEstadoInOrderByFechaAsc(Collection<EstadoOrden> estados);

    /** Órdenes por mecánico responsable desde una fecha: [id mecánico, cantidad]. */
    @Query("SELECT o.mecanico.id, COUNT(o) FROM OrdenIngreso o WHERE o.mecanico IS NOT NULL AND o.fecha >= :desde GROUP BY o.mecanico.id")
    List<Object[]> contarPorMecanicoDesde(@Param("desde") LocalDateTime desde);
}
