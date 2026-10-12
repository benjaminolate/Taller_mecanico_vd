package com.taller.mecanico.controller;

import com.taller.mecanico.entity.Usuario;
import com.taller.mecanico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Deja disponible en todas las pantallas el nombre del usuario conectado (para el menú). */
@ControllerAdvice
@RequiredArgsConstructor
public class UsuarioActualAdvice {

    private final UsuarioRepository usuarioRepo;

    @ModelAttribute("nombreUsuarioActual")
    public String nombreUsuarioActual(Authentication auth) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return usuarioRepo.findByUsernameIgnoreCase(auth.getName())
                .map(Usuario::getNombreCompleto)
                .orElse(auth.getName());
    }
}
