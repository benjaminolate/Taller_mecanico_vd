package com.taller.mecanico.controller;

import com.taller.mecanico.dto.ClienteResumen;
import com.taller.mecanico.dto.OrdenBusquedaDTO;
import com.taller.mecanico.dto.OrdenIngresoRequest;
import com.taller.mecanico.dto.VehiculoResumen;
import com.taller.mecanico.entity.Mecanico;
import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.enums.VistaHistorial;
import com.taller.mecanico.enums.MomentoFirma;
import com.taller.mecanico.enums.TipoIngreso;
import com.taller.mecanico.enums.TipoVehiculo;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.repository.ClienteRepository;
import com.taller.mecanico.repository.MecanicoRepository;
import com.taller.mecanico.repository.OrdenIngresoRepository;
import com.taller.mecanico.repository.VehiculoRepository;
import com.taller.mecanico.service.OrdenIngresoService;
import com.taller.mecanico.service.FirmaService;
import com.taller.mecanico.service.PdfService;
import com.taller.mecanico.util.Formatos;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/ordenes")
@Slf4j
public class OrdenIngresoController {

    private final OrdenIngresoService    ordenIngresoService;
    private final OrdenIngresoRepository ordenIngresoRepository;
    private final ClienteRepository      clienteRepository;
    private final VehiculoRepository     vehiculoRepository;
    private final MecanicoRepository     mecanicoRepository;
    private final PdfService             pdfService;
    private final FirmaService           firmaService;

    // ── Redirigir /ordenes → /ordenes/lista ─────────────────────────────────
    @GetMapping({"", "/"})
    public String inicio() {
        return "redirect:/ordenes/lista";
    }

    // ── Formulario de nueva orden ────────────────────────────────────────────
    @GetMapping("/nueva")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("ordenRequest", new OrdenIngresoRequest());
        cargarListasFormulario(model);
        return "ordenes/nueva-orden";
    }

    // ── Guardar orden (POST con validación) ──────────────────────────────────
    @PostMapping("/guardar")
    public String guardarOrden(
            @Valid @ModelAttribute("ordenRequest") OrdenIngresoRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            cargarListasFormulario(model);
            return "ordenes/nueva-orden";
        }
        try {
            OrdenIngreso ordenCreada = ordenIngresoService.crearOrdenCompleta(request);
            redirectAttributes.addFlashAttribute("ordenId", ordenCreada.getId());
            return "redirect:/ordenes/" + ordenCreada.getId() + "/confirmada";
        } catch (NegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al guardar una orden", e);
            model.addAttribute("error",
                    "No se pudo guardar la orden. Intenta nuevamente y, si se repite, avisa al administrador.");
        }
        cargarListasFormulario(model);
        return "ordenes/nueva-orden";
    }

    // ── Página de confirmación tras guardar ──────────────────────────────────
    @GetMapping("/{id}/confirmada")
    public String ordenConfirmada(@PathVariable String id, Model model) {
        ordenIngresoRepository.findById(id).ifPresent(o -> model.addAttribute("orden", o));
        return "ordenes/orden-confirmada";
    }

    // ── Lista de órdenes con búsqueda avanzada multi-criterio + paginación ───
    @GetMapping("/lista")
    public String listarOrdenes(
            @ModelAttribute("criterios") OrdenBusquedaDTO criterios,
            Model model,
            jakarta.servlet.http.HttpServletRequest request) {

        Pageable pageable = PageRequest.of(
                Math.max(criterios.getPage(), 0),
                criterios.getSize() > 0 ? criterios.getSize() : 10,
                Sort.by(criterios.getVistaEfectiva().isMasAntiguasPrimero()
                        ? Sort.Direction.ASC : Sort.Direction.DESC, "fecha"));

        Page<OrdenIngreso> resultado = ordenIngresoService.buscarOrdenes(criterios, pageable);

        model.addAttribute("ordenesPage", resultado);
        model.addAttribute("ordenes",     resultado.getContent());
        model.addAttribute("vistas",      VistaHistorial.values());
        model.addAttribute("vistaActual", criterios.getVistaEfectiva());
        model.addAttribute("conteoVistas", ordenIngresoService.contarPorVista(criterios));
        model.addAttribute("mecanicos",   mecanicoRepository.findAllByOrderByNombreAsc());
        model.addAttribute("mecanicosActivos", mecanicoRepository.findByEstadoActivoTrueOrderByNombreAsc());
        model.addAttribute("criterios",   criterios);
        // Al asignar un mecánico desde la lista se vuelve a la misma pestaña, filtros y página
        model.addAttribute("urlActual", request.getRequestURI()
                + (request.getQueryString() != null ? "?" + request.getQueryString() : ""));
        return "ordenes/lista-ordenes";
    }

    // ── Detalle de una orden ──────────────────────────────────────────────────
    @GetMapping("/{id}")
    public String verDetalleOrden(@PathVariable String id, Model model) {
        OrdenIngreso orden = ordenIngresoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + id));
        model.addAttribute("orden", orden);
        model.addAttribute("firmas", firmaService.vigentes(id));
        model.addAttribute("momentosFirma", MomentoFirma.values());
        model.addAttribute("mecanicosActivos", mecanicoRepository.findByEstadoActivoTrueOrderByNombreAsc());
        incluirMecanicoActual(orden, model);
        return "ordenes/detalle-orden";
    }

    // ── Asignar el mecánico que realiza el trabajo ───────────────────────────
    /**
     * Desde el historial o el detalle. {@code volver} es la pantalla a la que se
     * regresa (solo se aceptan rutas internas de órdenes).
     */
    @PostMapping("/{id}/mecanico")
    public String asignarMecanico(@PathVariable String id,
                                  @RequestParam(required = false) Long mecanicoId,
                                  @RequestParam(required = false) String volver,
                                  RedirectAttributes ra) {
        try {
            OrdenIngreso orden = ordenIngresoService.asignarMecanico(id, mecanicoId);
            ra.addFlashAttribute("mensajeExito", orden.getMecanico() == null
                    ? "La orden " + id + " quedó sin mecánico asignado."
                    : "La orden " + id + " quedó asignada a " + orden.getMecanico().getNombre() + ".");
        } catch (NegocioException e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        boolean volverValido = volver != null
                && (volver.equals("/") || volver.startsWith("/ordenes"))
                && !volver.contains("//") && !volver.contains("\\");
        return "redirect:" + (volverValido ? volver : "/ordenes/" + id);
    }

    // ── Editar una orden ─────────────────────────────────────────────────────
    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable String id, Model model) {
        OrdenIngreso orden = ordenIngresoService.buscar(id);
        if (orden.getEstado().esFinal()) {
            throw new NegocioException("La orden " + id + " ya fue entregada y no se puede editar.");
        }
        model.addAttribute("orden", orden);
        model.addAttribute("ordenRequest", ordenIngresoService.formularioEdicion(id));
        cargarListasFormulario(model);
        incluirMecanicoActual(orden, model);
        return "ordenes/editar-orden";
    }

    @PostMapping("/{id}/editar")
    public String editar(@PathVariable String id,
                         @ModelAttribute("ordenRequest") OrdenIngresoRequest request,
                         Model model,
                         RedirectAttributes ra) {
        try {
            ordenIngresoService.editarOrden(id, request);
            ra.addFlashAttribute("mensajeExito", "Orden actualizada.");
            return "redirect:/ordenes/" + id;
        } catch (NegocioException e) {
            model.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al editar la orden {}", id, e);
            model.addAttribute("error",
                    "No se pudo guardar la orden. Intenta nuevamente y, si se repite, avisa al administrador.");
        }
        OrdenIngreso orden = ordenIngresoService.buscar(id);
        model.addAttribute("orden", orden);
        cargarListasFormulario(model);
        incluirMecanicoActual(orden, model);
        return "ordenes/editar-orden";
    }

    // ── Generar PDF de una orden ──────────────────────────────────────────────
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generarPdf(@PathVariable String id) {
        OrdenIngreso orden = ordenIngresoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + id));

        byte[] pdf = pdfService.generarPdfOrden(orden);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "orden-" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    // ── JSON para el autocompletado del formulario de nueva orden ────────────

    @GetMapping("/api/clientes/{id}")
    @ResponseBody
    public ResponseEntity<ClienteResumen> obtenerClientePorIdApi(@PathVariable Long id) {
        return clienteRepository.findById(id)
                .map(ClienteResumen::de)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/vehiculos/patente/{patente}")
    @ResponseBody
    public ResponseEntity<VehiculoResumen> obtenerVehiculoPorPatenteApi(@PathVariable String patente) {
        String normalizada = Formatos.normalizarPatente(patente);
        if (normalizada == null) {
            return ResponseEntity.notFound().build();
        }
        return vehiculoRepository.findByPatente(normalizada)
                .map(VehiculoResumen::de)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Si un mecánico de la orden ya está inactivo, igual debe aparecer en las listas al editar. */
    private void incluirMecanicoActual(OrdenIngreso orden, Model model) {
        @SuppressWarnings("unchecked")
        List<Mecanico> lista = new ArrayList<>((List<Mecanico>) model.getAttribute("mecanicosActivos"));
        for (Mecanico actual : java.util.Arrays.asList(orden.getRecibidoPor(), orden.getMecanico())) {
            if (actual != null && lista.stream().noneMatch(m -> m.getId().equals(actual.getId()))) {
                lista.add(actual);
            }
        }
        model.addAttribute("mecanicosActivos", lista);
    }

    private void cargarListasFormulario(Model model) {
        model.addAttribute("mecanicosActivos", mecanicoRepository.findByEstadoActivoTrueOrderByNombreAsc());
        model.addAttribute("clientes",      clienteRepository.findAllByOrderByNombreAsc());
        model.addAttribute("tiposIngreso",  TipoIngreso.values());
        model.addAttribute("tiposVehiculo", TipoVehiculo.values());
    }
}
