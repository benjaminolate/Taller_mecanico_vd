package com.taller.mecanico.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Activa el registro automático de quién creó y modificó órdenes y trabajos. */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "usuarioConectado")
public class AuditoriaConfig {

    @Bean
    public AuditorAware<String> usuarioConectado() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                return Optional.empty();
            }
            return Optional.of(auth.getName());
        };
    }
}
