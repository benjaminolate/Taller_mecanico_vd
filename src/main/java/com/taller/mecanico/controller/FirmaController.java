package com.taller.mecanico.controller;

import com.taller.mecanico.entity.FirmaOrden;
import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.enums.MomentoFirma;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.service.FirmaService;
import com.taller.mecanico.service.OrdenIngresoService;
import com.taller.mecanico.service.TrabajoRealizadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Firmas en la tablet. /firmas muestra las órdenes pendientes de firma;
 * la pantalla de firma muestra el resumen de la orden y un recuadro para firmar con el dedo.
 */
@Controller
@RequiredArgsConstructor
public class FirmaController {

    private final FirmaService            firmaService;
    private final OrdenIngresoService     ordenService;
    private final TrabajoRealizadoService trabajoService;

    @GetMapping("/firmas")
    public String pendientes(Model model) {
        model.addAttribute("pendientesRecepcion", firmaService.pendientes(MomentoFirma.RECEPCION));
        model.addAttribute("pendientesRetiro", firmaService.pendientes(MomentoFirma.RETIRO));
        return "firmas/pendientes";
    }

    @GetMapping("/ordenes/{id}/firmar/{momento}")
    public String pantallaFirma(@PathVariable String id,
                                @PathVariable String momento,
                                @RequestParam(required = false) String volver,
                                Model model) {
        MomentoFirma m = parsear(momento);
        OrdenIngreso orden = ordenService.buscar(id);
        firmaService.verificarQueSePuedeFirmar(orden, m);
        cargarPantalla(orden, m, volver, model);
        return "firmas/firmar";
    }

    @PostMapping("/ordenes/{id}/firmar/{momento}")
    public String registrar(@PathVariable String id,
                            @PathVariable String momento,
                            @RequestParam(defaultValue = "firmar") String accion,
                            @RequestParam(required = false) String imagen,
                            @RequestParam(required = false) String nombre,
                            @RequestParam(required = false) String rut,
                            @RequestParam(required = false) String volver,
                            Model model,
                            RedirectAttributes ra) {
        MomentoFirma m = parsear(momento);
        boolean sinFirma = "sin-firma".equals(accion);
        try {
            if (!sinFirma && (imagen == null || imagen.isBlank())) {
                // Pidió guardar firma pero el recuadro llegó vacío: no se registra como "sin firma"
                throw new NegocioException("Firma en el recuadro o elige \"Continuar sin firma\".");
            }
            firmaService.registrar(id, m, sinFirma ? null : imagen, nombre, rut);
            ra.addFlashAttribute("mensajeExito", sinFirma
                    ? "Orden " + id + ": " + m.getEtiqueta().toLowerCase() + " registrada sin firma."
                    : "Orden " + id + ": firma de " + m.getEtiqueta().toLowerCase() + " guardada.");
            return "redirect:" + destino(volver, id);
        } catch (NegocioException e) {
            model.addAttribute("error", e.getMessage());
            OrdenIngreso orden = ordenService.buscar(id);
            cargarPantalla(orden, m, volver, model);
            model.addAttribute("nombre", nombre);
            model.addAttribute("rut", rut);
            return "firmas/firmar";
        }
    }

    @PostMapping("/firmas/{firmaId}/anular")
    public String anular(@PathVariable Long firmaId, RedirectAttributes ra) {
        FirmaOrden f = firmaService.anular(firmaId);
        ra.addFlashAttribute("mensajeExito", "Se anuló la " + f.getMomento().getEtiqueta().toLowerCase()
                + ". Ya puedes registrarla de nuevo.");
        return "redirect:/ordenes/" + f.getOrden().getId();
    }

    private void cargarPantalla(OrdenIngreso orden, MomentoFirma m, String volver, Model model) {
        model.addAttribute("orden", orden);
        model.addAttribute("momento", m);
        model.addAttribute("volver", destino(volver, orden.getId()));
        if (m == MomentoFirma.RETIRO) {
            model.addAttribute("trabajos", trabajoService.listarPorOrden(orden.getId()));
            model.addAttribute("costoTotal", trabajoService.calcularCostoTotal(orden.getId()));
        }
        if (!model.containsAttribute("nombre")) {
            model.addAttribute("nombre", orden.getCliente().getNombre());
        }
        if (!model.containsAttribute("rut")) {
            model.addAttribute("rut", orden.getCliente().getRut());
        }
    }

    /** Solo se vuelve a la lista de firmas o al detalle de la orden (nunca a otra dirección). */
    private String destino(String volver, String ordenId) {
        return "/firmas".equals(volver) ? "/firmas" : "/ordenes/" + ordenId;
    }

    private MomentoFirma parsear(String momento) {
        try {
            return MomentoFirma.valueOf(momento.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NegocioException("Tipo de firma desconocido: " + momento);
        }
    }
}
