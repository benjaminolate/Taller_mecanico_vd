package com.taller.mecanico.service;

import com.taller.mecanico.entity.Usuario;
import com.taller.mecanico.enums.Rol;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Cuentas del sistema: una del jefe y una compartida por los empleados.
 * El jefe cambia la contraseña de ambas desde la pantalla "Cuentas".
 * Las contraseñas tienen al menos {@value #LARGO_MINIMO} caracteres y se guardan cifradas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    public static final int LARGO_MINIMO = 6;

    /** Nombre de usuario: de 3 a 30 letras minúsculas, números, punto, guion o guion bajo. */
    private static final java.util.regex.Pattern FORMATO_USERNAME =
            java.util.regex.Pattern.compile("^[a-z0-9._-]{3,30}$");

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder   passwordEncoder;

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepo.findAllByOrderByActivoDescNombreCompletoAsc();
    }

    @Transactional(readOnly = true)
    public Usuario buscar(Long id) {
        return usuarioRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cuenta no encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepo.findByUsernameIgnoreCase(username);
    }

    @Transactional(readOnly = true)
    public boolean existeCuentaConRol(Rol rol) {
        return usuarioRepo.existsByRol(rol);
    }

    /**
     * El jefe cambia el nombre de usuario de una cuenta (la suya o la del empleado).
     * Se guarda en minúsculas y no puede repetirse. Devuelve el nombre anterior.
     */
    @Transactional
    public String cambiarUsername(Long id, String nuevo) {
        String limpio = nuevo == null ? "" : nuevo.trim().toLowerCase();
        if (!FORMATO_USERNAME.matcher(limpio).matches()) {
            throw new NegocioException("El nombre de usuario debe tener entre 3 y 30 caracteres: "
                    + "letras sin tildes, números, punto, guion o guion bajo, sin espacios.");
        }
        Usuario u = buscar(id);
        String anterior = u.getUsername();
        if (limpio.equals(anterior)) {
            throw new NegocioException("La cuenta ya se llama \"" + limpio + "\".");
        }
        usuarioRepo.findByUsernameIgnoreCase(limpio)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new NegocioException("Ya existe otra cuenta con el usuario \"" + limpio + "\".");
                });
        u.setUsername(limpio);
        usuarioRepo.save(u);
        log.info("Nombre de usuario cambiado: {} -> {}", anterior, limpio);
        return anterior;
    }

    /** Crea una cuenta (se usa al iniciar la app por primera vez). */
    @Transactional
    public Usuario crear(String username, String nombreCompleto, Rol rol, String password) {
        validarPassword(password);
        Usuario u = new Usuario();
        u.setUsername(username.trim().toLowerCase());
        u.setNombreCompleto(nombreCompleto);
        u.setRol(rol);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setActivo(true);
        u.setDebeCambiarPassword(false);
        Usuario guardado = usuarioRepo.save(u);
        log.info("Cuenta creada: {} ({})", guardado.getUsername(), rol);
        return guardado;
    }

    /** El jefe cambia la contraseña de una cuenta (la suya o la del empleado). Queda definitiva. */
    @Transactional
    public void cambiarPassword(Long id, String nueva, String confirmacion) {
        if (nueva == null || !nueva.equals(confirmacion)) {
            throw new NegocioException("La nueva contraseña y su confirmación no coinciden.");
        }
        validarPassword(nueva);
        Usuario u = buscar(id);
        u.setPasswordHash(passwordEncoder.encode(nueva));
        u.setDebeCambiarPassword(false);
        usuarioRepo.save(u);
        log.info("Contraseña cambiada para la cuenta {}", u.getUsername());
    }

    private void validarPassword(String password) {
        if (password == null || password.length() < LARGO_MINIMO) {
            throw new NegocioException("La contraseña debe tener al menos " + LARGO_MINIMO + " caracteres.");
        }
    }
}
