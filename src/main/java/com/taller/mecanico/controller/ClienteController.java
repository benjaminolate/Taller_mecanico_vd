package com.taller.mecanico.controller;

import com.taller.mecanico.entity.Cliente;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Clientes: búsqueda, edición e historial. Jefe y empleados. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        Page<Cliente> clientes = clienteService.buscar(q,
                PageRequest.of(Math.max(page, 0), 20, Sort.by("nombre")));
        model.addAttribute("clientesPage", clientes);
        model.addAttribute("q", q);
        return "clientes/lista-clientes";
    }

    @GetMapping("/{id}")
    public String historial(@PathVariable Long id, Model model) {
        model.addAttribute("cliente", clienteService.buscar(id));
        model.addAttribute("ordenes", clienteService.historial(id));
        return "clientes/historial-cliente";
    }

    @GetMapping("/{id}/editar")
    public String formularioEditar(@PathVariable Long id, Model model) {
        model.addAttribute("cliente", clienteService.buscar(id));
        return "clientes/form-cliente";
    }

    @PostMapping("/{id}/editar")
    public String actualizar(@PathVariable Long id,
                             @RequestParam String nombre,
                             @RequestParam(required = false) String telefono,
                             @RequestParam(required = false) String rut,
                             @RequestParam(required = false) String relacionVehiculo,
                             Model model,
                             RedirectAttributes ra) {
        try {
            clienteService.actualizar(id, nombre, telefono, rut, relacionVehiculo);
            ra.addFlashAttribute("mensajeExito", "Datos del cliente actualizados.");
            return "redirect:/clientes/" + id;
        } catch (NegocioException e) {
            Cliente c = new Cliente();
            c.setId(id);
            c.setNombre(nombre);
            c.setTelefono(telefono);
            c.setRut(rut);
            c.setRelacionVehiculo(relacionVehiculo);
            model.addAttribute("cliente", c);
            model.addAttribute("error", e.getMessage());
            return "clientes/form-cliente";
        }
    }
}
