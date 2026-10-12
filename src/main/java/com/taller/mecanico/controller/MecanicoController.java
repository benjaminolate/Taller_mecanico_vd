package com.taller.mecanico.controller;

import com.taller.mecanico.entity.Mecanico;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.service.MecanicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Catálogo de mecánicos. Solo el jefe. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/mecanicos")
@PreAuthorize("hasRole('JEFE')")
public class MecanicoController {

    private final MecanicoService mecanicoService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("mecanicos", mecanicoService.listar());
        return "mecanicos/lista-mecanicos";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("mecanico", new Mecanico());
        return "mecanicos/form-mecanico";
    }

    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable Long id, Model model) {
        model.addAttribute("mecanico", mecanicoService.buscar(id));
        return "mecanicos/form-mecanico";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam(required = false) Long id,
                          @RequestParam String nombre,
                          @RequestParam(required = false) String rut,
                          @RequestParam(defaultValue = "false") boolean activo,
                          Model model,
                          RedirectAttributes ra) {
        try {
            Mecanico m = mecanicoService.guardar(id, nombre, rut, activo);
            ra.addFlashAttribute("mensajeExito", "Mecánico \"" + m.getNombre() + "\" guardado.");
            return "redirect:/mecanicos";
        } catch (NegocioException e) {
            Mecanico m = new Mecanico();
            m.setId(id);
            m.setNombre(nombre);
            m.setRut(rut);
            m.setEstadoActivo(activo);
            model.addAttribute("mecanico", m);
            model.addAttribute("error", e.getMessage());
            return "mecanicos/form-mecanico";
        }
    }
}
