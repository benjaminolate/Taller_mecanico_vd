package com.taller.mecanico.controller;

import com.taller.mecanico.entity.CatalogoTrabajo;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.service.CatalogoTrabajoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/** Catálogo de tareas con precio sugerido. Solo el jefe. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/catalogo")
@PreAuthorize("hasRole('JEFE')")
public class CatalogoTrabajoController {

    private final CatalogoTrabajoService catalogoService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("tareas", catalogoService.listar());
        return "catalogo/lista-catalogo";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("tarea", new CatalogoTrabajo());
        return "catalogo/form-catalogo";
    }

    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable Long id, Model model) {
        model.addAttribute("tarea", catalogoService.buscar(id));
        return "catalogo/form-catalogo";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam(required = false) Long id,
                          @RequestParam String nombreTarea,
                          @RequestParam(required = false) BigDecimal precio,
                          @RequestParam(defaultValue = "false") boolean precioIncluyeIva,
                          @RequestParam(defaultValue = "false") boolean activo,
                          Model model,
                          RedirectAttributes ra) {
        try {
            CatalogoTrabajo c = catalogoService.guardar(id, nombreTarea, precio, precioIncluyeIva, activo);
            ra.addFlashAttribute("mensajeExito", "Tarea \"" + c.getNombreTarea() + "\" guardada.");
            return "redirect:/catalogo";
        } catch (NegocioException e) {
            CatalogoTrabajo c = new CatalogoTrabajo();
            c.setId(id);
            c.setNombreTarea(nombreTarea);
            c.setCostoEstimadoDefecto(precio);
            c.setPrecioIncluyeIva(precioIncluyeIva);
            c.setActivo(activo);
            model.addAttribute("tarea", c);
            model.addAttribute("error", e.getMessage());
            return "catalogo/form-catalogo";
        }
    }
}
