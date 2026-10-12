package com.taller.mecanico.controller;

import com.taller.mecanico.dto.TrabajoRealizadoRequest;
import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.entity.TrabajoRealizado;
import com.taller.mecanico.enums.CategoriaTrabajo;
import com.taller.mecanico.enums.EstadoOrden;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.service.CatalogoTrabajoService;
import com.taller.mecanico.repository.MecanicoRepository;
import com.taller.mecanico.repository.OrdenIngresoRepository;
import com.taller.mecanico.service.TrabajoRealizadoService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controlador MVC para la gestión de trabajos técnicos por orden.
 * URL base: /ordenes/{ordenId}/trabajos
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/ordenes/{ordenId}/trabajos")
@Slf4j
public class TrabajoRealizadoController {

    private final TrabajoRealizadoService   trabajoService;
    private final OrdenIngresoRepository    ordenRepo;
    private final MecanicoRepository        mecanicoRepo;
    private final CatalogoTrabajoService    catalogoService;

    @GetMapping
    public String listarTrabajos(@PathVariable String ordenId, Model model) {
        cargarModeloTrabajos(ordenId, model);
        TrabajoRealizadoRequest nuevo = new TrabajoRealizadoRequest();
        OrdenIngreso orden = (OrdenIngreso) model.getAttribute("orden");
        if (orden != null && orden.getMecanico() != null) {
            nuevo.setMecanicoId(orden.getMecanico().getId()); // por defecto, el responsable de la orden
        }
        model.addAttribute("nuevoTrabajo", nuevo);
        return "trabajos/lista-trabajos";
    }

    @PostMapping("/guardar")
    public String guardarTrabajo(
            @PathVariable String ordenId,
            @Valid @ModelAttribute("nuevoTrabajo") TrabajoRealizadoRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        request.setOrdenId(ordenId);

        if (bindingResult.hasErrors()) {
            cargarModeloTrabajos(ordenId, model);
            return "trabajos/lista-trabajos";
        }
        ejecutar(redirectAttributes, "Trabajo registrado correctamente.",
                "No se pudo registrar el trabajo.", () -> trabajoService.registrar(request));
        return "redirect:/ordenes/" + ordenId + "/trabajos";
    }

    @GetMapping("/{trabajoId}/editar")
    public String formularioEditar(@PathVariable String ordenId, @PathVariable Long trabajoId, Model model) {
        TrabajoRealizado t = trabajoService.buscarDeOrden(ordenId, trabajoId);
        if (t.getOrden().getEstado().esFinal()) {
            throw new NegocioException("La orden " + ordenId + " ya fue entregada y no admite cambios en sus trabajos.");
        }
        TrabajoRealizadoRequest r = new TrabajoRealizadoRequest();
        r.setDescripcion(t.getDescripcion());
        r.setCategoria(t.getCategoria());
        r.setMecanicoId(t.getMecanico() != null ? t.getMecanico().getId() : null);
        r.setCostoEstimado(t.getCostoEstimado());
        r.setObservaciones(t.getObservaciones());
        r.setCompletado(t.getCompletado());
        model.addAttribute("trabajoForm", r);
        cargarModeloEdicion(t, model);
        return "trabajos/editar-trabajo";
    }

    @PostMapping("/{trabajoId}/editar")
    public String editar(@PathVariable String ordenId,
                         @PathVariable Long trabajoId,
                         @Valid @ModelAttribute("trabajoForm") TrabajoRealizadoRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            cargarModeloEdicion(trabajoService.buscarDeOrden(ordenId, trabajoId), model);
            return "trabajos/editar-trabajo";
        }
        try {
            trabajoService.actualizar(ordenId, trabajoId, request);
            ra.addFlashAttribute("mensajeExito", "Trabajo actualizado.");
            return "redirect:/ordenes/" + ordenId + "/trabajos";
        } catch (NegocioException | EntityNotFoundException e) {
            model.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al editar el trabajo {}", trabajoId, e);
            model.addAttribute("error", "No se pudo guardar el trabajo. Intenta nuevamente y, si se repite, avisa al administrador.");
        }
        cargarModeloEdicion(trabajoService.buscarDeOrden(ordenId, trabajoId), model);
        return "trabajos/editar-trabajo";
    }

    @PostMapping("/{trabajoId}/completar")
    public String completarTrabajo(
            @PathVariable String ordenId,
            @PathVariable Long trabajoId,
            RedirectAttributes redirectAttributes) {

        ejecutar(redirectAttributes, "Trabajo marcado como completado.",
                "No se pudo completar el trabajo.", () -> trabajoService.marcarCompletado(ordenId, trabajoId));
        return "redirect:/ordenes/" + ordenId + "/trabajos";
    }

    @PostMapping("/{trabajoId}/eliminar")
    public String eliminarTrabajo(
            @PathVariable String ordenId,
            @PathVariable Long trabajoId,
            RedirectAttributes redirectAttributes) {

        ejecutar(redirectAttributes, "Trabajo eliminado.",
                "No se pudo eliminar el trabajo.", () -> trabajoService.eliminar(ordenId, trabajoId));
        return "redirect:/ordenes/" + ordenId + "/trabajos";
    }

    @PostMapping("/cambiar-estado")
    public String cambiarEstado(
            @PathVariable String ordenId,
            @RequestParam EstadoOrden nuevoEstado,
            RedirectAttributes redirectAttributes) {

        ejecutar(redirectAttributes, "Estado de la orden actualizado a: " + nuevoEstado.getEtiqueta(),
                "No se pudo cambiar el estado.", () -> trabajoService.cambiarEstadoOrden(ordenId, nuevoEstado));
        return "redirect:/ordenes/" + ordenId + "/trabajos";
    }

    // ── Privados ─────────────────────────────────────────────────────────────

    /**
     * Ejecuta la acción y deja el mensaje para la pantalla: el de la regla
     * del taller si falló una validación, o uno genérico (y se registra en
     * el log) si fue un error inesperado.
     */
    private void ejecutar(RedirectAttributes ra, String exito, String errorGenerico, Runnable accion) {
        try {
            accion.run();
            ra.addFlashAttribute("mensajeExito", exito);
        } catch (NegocioException | EntityNotFoundException e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado en trabajos: {}", errorGenerico, e);
            ra.addFlashAttribute("mensajeError",
                    errorGenerico + " Intenta nuevamente y, si se repite, avisa al administrador.");
        }
    }

    private void cargarModeloEdicion(TrabajoRealizado t, Model model) {
        model.addAttribute("orden", t.getOrden());
        model.addAttribute("trabajo", t);
        model.addAttribute("categorias", CategoriaTrabajo.values());
        // Activos, más el mecánico actual aunque esté inactivo, para no perderlo al guardar
        List<com.taller.mecanico.entity.Mecanico> mecanicos =
                new java.util.ArrayList<>(mecanicoRepo.findByEstadoActivoTrueOrderByNombreAsc());
        if (t.getMecanico() != null && mecanicos.stream().noneMatch(m -> m.getId().equals(t.getMecanico().getId()))) {
            mecanicos.add(t.getMecanico());
        }
        model.addAttribute("mecanicos", mecanicos);
    }

    private void cargarModeloTrabajos(String ordenId, Model model) {
        OrdenIngreso orden = ordenRepo.findById(ordenId)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + ordenId));
        List<TrabajoRealizado> trabajos = trabajoService.listarPorOrden(ordenId);
        long completados = trabajos.stream().filter(TrabajoRealizado::getCompletado).count();

        model.addAttribute("orden", orden);
        model.addAttribute("trabajos", trabajos);
        model.addAttribute("costoTotal", trabajoService.calcularCostoTotal(ordenId));
        model.addAttribute("estadosPermitidos", trabajoService.estadosPermitidosDesde(orden.getEstado()));
        model.addAttribute("categorias", CategoriaTrabajo.values());
        model.addAttribute("trabajosCompletados", completados);
        model.addAttribute("trabajosPendientes", trabajos.size() - completados);
        // Mecánicos activos para el <select> del formulario.
        model.addAttribute("mecanicos", mecanicoRepo.findByEstadoActivoTrueOrderByNombreAsc());
        // Catálogo de trabajos para el <select> con autocompletado.
        model.addAttribute("catalogoTrabajos", catalogoService.listarActivos());
    }
}
