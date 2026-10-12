package com.taller.mecanico.service;

import com.taller.mecanico.dto.ResumenMecanico;
import com.taller.mecanico.entity.Mecanico;
import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.enums.EstadoOrden;
import com.taller.mecanico.repository.MecanicoRepository;
import com.taller.mecanico.repository.OrdenIngresoRepository;
import com.taller.mecanico.repository.TrabajoRealizadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Datos del panel de inicio: cuántas órdenes hay en cada estado, cuáles están
 * listas para entregar, cuáles llevan demasiados días en el taller y la
 * actividad del mes por mecánico.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PanelService {

    /** Una orden sin entregar con más días que este se muestra como atrasada. */
    public static final int DIAS_ATRASO = 7;

    private final OrdenIngresoRepository     ordenRepository;
    private final TrabajoRealizadoRepository trabajoRepository;
    private final MecanicoRepository         mecanicoRepository;

    /** Cantidad de órdenes por estado, con el nombre del estado como clave ("PENDIENTE", ...). */
    public Map<String, Long> conteoPorEstado() {
        Map<String, Long> conteo = new java.util.LinkedHashMap<>();
        for (EstadoOrden estado : EstadoOrden.values()) {
            conteo.put(estado.name(), ordenRepository.countByEstado(estado));
        }
        return conteo;
    }

    public long ingresadasEsteMes() {
        return ordenRepository.countByFechaGreaterThanEqual(inicioDelMes());
    }

    public List<OrdenIngreso> listasParaEntregar() {
        return ordenRepository.findTop20ByEstadoOrderByFechaAsc(EstadoOrden.LISTO);
    }

    /** Pendientes o en proceso, ingresadas hace más de {@link #DIAS_ATRASO} días. */
    public List<OrdenIngreso> atrasadas() {
        LocalDateTime limite = LocalDate.now().minusDays(DIAS_ATRASO).atStartOfDay();
        return ordenRepository.findTop20ByEstadoInAndFechaBeforeOrderByFechaAsc(
                List.of(EstadoOrden.PENDIENTE, EstadoOrden.EN_PROCESO), limite);
    }

    /** Órdenes por hacer que todavía no tienen mecánico que realice el trabajo. */
    public List<OrdenIngreso> sinMecanico() {
        return ordenRepository.findTop20ByMecanicoIsNullAndEstadoInOrderByFechaAsc(
                List.of(EstadoOrden.PENDIENTE, EstadoOrden.EN_PROCESO));
    }

    /** Actividad del mes en curso por mecánico, de mayor a menor monto. */
    public List<ResumenMecanico> resumenMecanicosDelMes() {
        LocalDateTime desde = inicioDelMes();

        Map<Long, Long> ordenes = new HashMap<>();
        for (Object[] fila : ordenRepository.contarPorMecanicoDesde(desde)) {
            ordenes.put(((Number) fila[0]).longValue(), ((Number) fila[1]).longValue());
        }
        Map<Long, Object[]> trabajos = new HashMap<>();
        for (Object[] fila : trabajoRepository.resumenCompletadosPorMecanicoDesde(desde)) {
            trabajos.put(((Number) fila[0]).longValue(), fila);
        }

        List<ResumenMecanico> resumen = new ArrayList<>();
        for (Mecanico m : mecanicoRepository.findAll()) {
            long nOrdenes = ordenes.getOrDefault(m.getId(), 0L);
            Object[] t = trabajos.get(m.getId());
            long nTrabajos = t == null ? 0 : ((Number) t[1]).longValue();
            BigDecimal monto = t == null || t[2] == null ? BigDecimal.ZERO : new BigDecimal(t[2].toString());
            boolean activo = Boolean.TRUE.equals(m.getEstadoActivo());
            if (activo || nOrdenes > 0 || nTrabajos > 0) {
                resumen.add(new ResumenMecanico(m.getNombre(), nOrdenes, nTrabajos, monto));
            }
        }
        resumen.sort(Comparator.comparing(ResumenMecanico::montoTrabajos).reversed()
                .thenComparing(ResumenMecanico::nombre));
        return resumen;
    }

    private static LocalDateTime inicioDelMes() {
        return LocalDate.now().withDayOfMonth(1).atStartOfDay();
    }
}
