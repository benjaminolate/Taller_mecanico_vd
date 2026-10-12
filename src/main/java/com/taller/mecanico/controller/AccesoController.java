package com.taller.mecanico.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Pantalla de login y aviso de acceso denegado. */
@Controller
public class AccesoController {

    @GetMapping("/login")
    public String login(Authentication auth) {
        // Si ya inició sesión, no tiene sentido mostrarle el login otra vez
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        return "login";
    }

    @RequestMapping("/acceso-denegado")
    public String accesoDenegado(Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        model.addAttribute("titulo", "Sin permiso");
        model.addAttribute("mensaje", "Esta acción está reservada para el jefe del taller.");
        return "error/error";
    }
}
