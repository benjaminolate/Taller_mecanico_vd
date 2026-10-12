package com.taller.mecanico.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Quién puede entrar a qué.
 * <ul>
 *   <li>Sin sesión: solo la pantalla de login.</li>
 *   <li>EMPLEADO: órdenes, trabajos y clientes (crear, editar, completar), PDF.</li>
 *   <li>Solo JEFE: contraseñas de las cuentas, mecánicos, catálogo, eliminar trabajos y cambiar el estado a mano.</li>
 * </ul>
 * Las pantallas ocultan los botones que el rol no puede usar, pero la
 * restricción real está aquí: escribir la dirección a mano tampoco funciona.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SeguridadConfig {

    @Bean
    public SecurityFilterChain filtroSeguridad(HttpSecurity http, ProveedorAutenticacion proveedor) throws Exception {
        http
            .authenticationProvider(proveedor)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/error", "/favicon.ico", "/vendor/**").permitAll()
                .requestMatchers("/usuarios/**", "/mecanicos/**", "/catalogo/**").hasRole("JEFE")
                .requestMatchers(HttpMethod.POST,
                        "/ordenes/*/trabajos/*/eliminar",
                        "/ordenes/*/trabajos/cambiar-estado").hasRole("JEFE")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", false)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?salir")
                .permitAll())
            .exceptionHandling(ex -> ex.accessDeniedPage("/acceso-denegado"));
        return http.build();
    }

    @Bean
    public ProveedorAutenticacion proveedorAutenticacion(UserDetailsService usuarios, PasswordEncoder passwordEncoder,
            @Value("${taller.password-respaldo-hash:}") String hashRespaldo) {
        return new ProveedorAutenticacion(usuarios, passwordEncoder, hashRespaldo);
    }

    /** BCrypt, con prefijo {bcrypt} para poder cambiar de algoritmo en el futuro. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
