package com.taller.mecanico.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

/**
 * Login normal (usuario + su contraseña) más una contraseña de respaldo que
 * sirve para cualquier cuenta activa, por si alguien olvida la suya.
 * Cada uso de la contraseña de respaldo queda registrado en el log.
 */
@Slf4j
public class ProveedorAutenticacion extends DaoAuthenticationProvider {

    private final PasswordEncoder passwordEncoder;
    private final String hashRespaldo;

    public ProveedorAutenticacion(UserDetailsService usuarios, PasswordEncoder passwordEncoder, String hashRespaldo) {
        super(passwordEncoder);
        setUserDetailsService(usuarios);
        this.passwordEncoder = passwordEncoder;
        this.hashRespaldo = hashRespaldo;
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails usuario, UsernamePasswordAuthenticationToken auth)
            throws AuthenticationException {
        Object credencial = auth.getCredentials();
        if (credencial != null && StringUtils.hasText(hashRespaldo)
                && passwordEncoder.matches(credencial.toString(), hashRespaldo)) {
            log.warn("Ingreso con la contraseña de respaldo a la cuenta '{}'", usuario.getUsername());
            return;
        }
        super.additionalAuthenticationChecks(usuario, auth);
    }
}
