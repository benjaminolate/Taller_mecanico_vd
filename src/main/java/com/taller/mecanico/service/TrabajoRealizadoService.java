package com.taller.mecanico.service;

import com.taller.mecanico.dto.TrabajoRealizadoRequest;
import com.taller.mecanico.entity.Mecanico;
import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.entity.TrabajoRealizado;
import com.taller.mecanico.enums.EstadoOrden;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.repository.MecanicoRepository;
import com.taller.mecanico.repository.OrdenIngresoRepository;
import com.taller.mecanico.repository.TrabajoRealizadoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Trabajos de una orden y los cambios de estado que provocan:
 * <ul>
 *   <li>Agregar un trabajo pendiente pasa la orden a EN_PROCESO (desde PENDIENTE o LISTO).</li>
 *   <li>Completar o eliminar el último pendiente pasa la orden a LISTO.</li>
 *   <li>Una orden ENTREGADA no admite cambios en sus trabajos.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TrabajoRealizadoService {

    private final TrabajoRealizadoRepository trabajoRepo;
    private final OrdenIngresoRepository     ordenRepo;
    private final MecanicoRepository         mecanicoRepo;

    @Transactional(readOnly = true)
    public List<TrabajoRealizado> listarPorOrden(String ordenId) {
        return trabajoRepo.findByOrdenIdOrderByFechaRegistroAsc(ordenId);
    }

    @Transactional(readOnly = true)
    public TrabajoRealizado buscarPorId(Long id) {
        return trabajoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trabajo no encontrado con ID: " + id));
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularCostoTotal(String ordenId) {
        BigDecimal total = trabajoRepo.sumCostoEstimadoByOrdenId(ordenId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional
    public TrabajoRealizado registrar(TrabajoRealizadoRequest request) {
        OrdenIngreso orden = buscarOrden(request.getOrdenId());
        exigirEditable(orden);

        TrabajoRealizado trabajo = new TrabajoRealizado();
        mapRequestToEntidad(request, trabajo);
        trabajo.setOrden(orden);
        if (Boolean.TRUE.equals(trabajo.getCompletado())) {
            trabajo.setFechaCompletado(LocalDateTime.now());
        }
        TrabajoRealizado guardado = trabajoRepo.save(trabajo);

        if (!Boolean.TRUE.equals(guardado.getCompletado())
                && (orden.getEstado() == EstadoOrden.PENDIENTE || orden.getEstado() == EstadoOrden.LISTO)) {
            orden.setEstado(EstadoOrden.EN_PROCESO);
            ordenRepo.save(orden);
        }
        return guardado;
    }

    @Transactional(readOnly = true)
    public TrabajoRealizado buscarDeOrden(String ordenId, Long id) {
        return buscarPorIdYOrden(ordenId, id);
    }

    /**
     * Corrige un trabajo (descripción, categoría, mecánico, costo, observaciones,
     * completado). Recalcula el estado de la orden si cambió "completado".
     */
    @Transactional
    public TrabajoRealizado actualizar(String ordenId, Long id, TrabajoRealizadoRequest request) {
        TrabajoRealizado trabajo = buscarPorIdYOrden(ordenId, id);
        OrdenIngreso orden = trabajo.getOrden();
        exigirEditable(orden);
        boolean estabaCompletado = Boolean.TRUE.equals(trabajo.getCompletado());
        mapRequestToEntidad(request, trabajo);
        boolean quedaCompletado = Boolean.TRUE.equals(trabajo.getCompletado());
        if (quedaCompletado && !estabaCompletado) {
            trabajo.setFechaCompletado(LocalDateTime.now());
        } else if (!quedaCompletado) {
            trabajo.setFechaCompletado(null);
        }
        TrabajoRealizado guardado = trabajoRepo.save(trabajo);

        if (!quedaCompletado && (orden.getEstado() == EstadoOrden.LISTO || orden.getEstado() == EstadoOrden.PENDIENTE)) {
            orden.setEstado(EstadoOrden.EN_PROCESO);
            ordenRepo.save(orden);
        } else {
            actualizarSiQuedoLista(orden);
        }
        return guardado;
    }

    @Transactional
    public TrabajoRealizado marcarCompletado(String ordenId, Long id) {
        TrabajoRealizado trabajo = buscarPorIdYOrden(ordenId, id);
        exigirEditable(trabajo.getOrden());
        trabajo.setCompletado(true);
        trabajo.setFechaCompletado(LocalDateTime.now());
        TrabajoRealizado guardado = trabajoRepo.save(trabajo);
        actualizarSiQuedoLista(guardado.getOrden());
        return guardado;
    }

    /**
     * Cambio manual de estado. Solo se permiten los cambios definidos en
     * {@link EstadoOrden#getSiguientesPermitidos()}, y LISTO exige que no
     * queden trabajos pendientes.
     */
    @Transactional
    public OrdenIngreso cambiarEstadoOrden(String ordenId, EstadoOrden nuevoEstado) {
        OrdenIngreso orden = buscarOrden(ordenId);
        EstadoOrden actual = orden.getEstado();

        if (actual == nuevoEstado) {
            throw new NegocioException("La orden ya está en estado " + actual.getEtiqueta() + ".");
        }
        if (!actual.puedeCambiarA(nuevoEstado)) {
            throw new NegocioException("No se puede pasar una orden de "
                    + actual.getEtiqueta() + " a " + nuevoEstado.getEtiqueta() + ".");
        }
        if (nuevoEstado == EstadoOrden.LISTO) {
            int pendientes = trabajoRepo.findByOrdenIdAndCompletadoFalse(ordenId).size();
            if (pendientes > 0) {
                throw new NegocioException("No se puede marcar como Listo: quedan "
                        + pendientes + (pendientes == 1 ? " trabajo pendiente." : " trabajos pendientes."));
            }
        }
        orden.setEstado(nuevoEstado);
        return ordenRepo.save(orden);
    }

    @Transactional
    public void eliminar(String ordenId, Long id) {
        TrabajoRealizado trabajo = buscarPorIdYOrden(ordenId, id);
        OrdenIngreso orden = trabajo.getOrden();
        exigirEditable(orden);
        orden.getTrabajos().remove(trabajo);
        trabajoRepo.delete(trabajo);
        trabajoRepo.flush();
        actualizarSiQuedoLista(orden);
    }

    // ── Privados ─────────────────────────────────────────────────────────────

    private OrdenIngreso buscarOrden(String ordenId) {
        return ordenRepo.findById(ordenId)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + ordenId));
    }

    private void exigirEditable(OrdenIngreso orden) {
        if (orden.getEstado().esFinal()) {
            throw new NegocioException("La orden " + orden.getId()
                    + " ya fue entregada y no admite cambios en sus trabajos.");
        }
    }

    /** Si la orden está EN_PROCESO, tiene trabajos y ninguno pendiente, pasa a LISTO. */
    private void actualizarSiQuedoLista(OrdenIngreso orden) {
        if (orden.getEstado() != EstadoOrden.EN_PROCESO) return;
        boolean tieneTrabajos = trabajoRepo.countByOrdenId(orden.getId()) > 0;
        boolean sinPendientes = trabajoRepo.findByOrdenIdAndCompletadoFalse(orden.getId()).isEmpty();
        if (tieneTrabajos && sinPendientes) {
            orden.setEstado(EstadoOrden.LISTO);
            ordenRepo.save(orden);
        }
    }

    private TrabajoRealizado buscarPorIdYOrden(String ordenId, Long id) {
        TrabajoRealizado trabajo = buscarPorId(id);
        if (!trabajo.getOrden().getId().equals(ordenId)) {
            throw new EntityNotFoundException("Trabajo " + id + " no pertenece a la orden " + ordenId);
        }
        return trabajo;
    }

    private void mapRequestToEntidad(TrabajoRealizadoRequest request, TrabajoRealizado entidad) {
        entidad.setDescripcion(request.getDescripcion().trim());
        entidad.setCategoria(request.getCategoria());
        entidad.setCostoEstimado(request.getCostoEstimado());
        entidad.setObservaciones(request.getObservaciones());
        entidad.setCompletado(request.getCompletado() != null && request.getCompletado());
        if (request.getMecanicoId() != null) {
            Mecanico mecanico = mecanicoRepo.findById(request.getMecanicoId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Mecánico no encontrado con ID: " + request.getMecanicoId()));
            entidad.setMecanico(mecanico);
        } else {
            entidad.setMecanico(null);
        }
    }

    /** Estados que se pueden elegir a mano desde la pantalla de trabajos, en orden. */
    public List<EstadoOrden> estadosPermitidosDesde(EstadoOrden actual) {
        return actual.getSiguientesPermitidos().stream()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .toList();
    }
}
