package com.taller.mecanico.repository;

import com.taller.mecanico.entity.FirmaOrden;
import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.enums.MomentoFirma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FirmaOrdenRepository extends JpaRepository<FirmaOrden, Long> {

    /** Firmas vigentes (no anuladas) de una orden. */
    List<FirmaOrden> findByOrdenIdAndAnuladaFalse(String ordenId);

    Optional<FirmaOrden> findByOrdenIdAndMomentoAndAnuladaFalse(String ordenId, MomentoFirma momento);

    /** Todas las firmas de una orden, incluidas las anuladas, de la más nueva a la más antigua. */
    List<FirmaOrden> findByOrdenIdOrderByIdDesc(String ordenId);

    /** Órdenes no entregadas que aún no tienen registro de firma de recepción. */
    @Query("""
            SELECT o FROM OrdenIngreso o
            WHERE o.estado <> com.taller.mecanico.enums.EstadoOrden.ENTREGADO
              AND NOT EXISTS (SELECT f FROM FirmaOrden f
                              WHERE f.orden = o AND f.momento = :momento AND f.anulada = false)
            ORDER BY o.fecha DESC
            """)
    List<OrdenIngreso> pendientesDeRecepcion(@Param("momento") MomentoFirma momento);

    /** Órdenes listas para retirar que aún no tienen registro de firma de retiro. */
    @Query("""
            SELECT o FROM OrdenIngreso o
            WHERE o.estado = com.taller.mecanico.enums.EstadoOrden.LISTO
              AND NOT EXISTS (SELECT f FROM FirmaOrden f
                              WHERE f.orden = o AND f.momento = :momento AND f.anulada = false)
            ORDER BY o.fecha DESC
            """)
    List<OrdenIngreso> pendientesDeRetiro(@Param("momento") MomentoFirma momento);
}
