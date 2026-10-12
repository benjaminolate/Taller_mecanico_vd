package com.taller.mecanico.controller;

import com.taller.mecanico.entity.Usuario;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Cuentas del sistema y sus contraseñas. Solo el jefe. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('JEFE')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("largoMinimo", UsuarioService.LARGO_MINIMO);
        return "usuarios/lista-usuarios";
    }

    /**
     * Cambia el nombre de usuario. Si el jefe cambia el de su propia cuenta,
     * se cierra su sesión para que vuelva a entrar con el nombre nuevo.
     */
    @PostMapping("/{id}/username")
    public String cambiarUsername(@PathVariable Long id,
                                  @RequestParam String nuevo,
                                  java.security.Principal principal,
                                  jakarta.servlet.http.HttpServletRequest request,
                                  RedirectAttributes ra) throws jakarta.servlet.ServletException {
        try {
            String anterior = usuarioService.cambiarUsername(id, nuevo);
            Usuario u = usuarioService.buscar(id);
            if (principal != null && principal.getName().equalsIgnoreCase(anterior)) {
                request.logout();
                return "redirect:/login?usuarioCambiado";
            }
            ra.addFlashAttribute("mensajeExito", "La cuenta \"" + anterior + "\" ahora entra con el usuario \""
                    + u.getUsername() + "\".");
        } catch (NegocioException e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/password")
    public String cambiarPassword(@PathVariable Long id,
                                  @RequestParam String nueva,
                                  @RequestParam String confirmacion,
                                  RedirectAttributes ra) {
        try {
            usuarioService.cambiarPassword(id, nueva, confirmacion);
            Usuario u = usuarioService.buscar(id);
            ra.addFlashAttribute("mensajeExito", "Contraseña de la cuenta \"" + u.getUsername() + "\" actualizada.");
        } catch (NegocioException e) {
            ra.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/usuarios";
    }
}
