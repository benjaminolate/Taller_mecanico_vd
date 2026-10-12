package com.taller.mecanico;

import com.taller.mecanico.entity.Usuario;
import com.taller.mecanico.enums.Rol;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.repository.UsuarioRepository;
import com.taller.mecanico.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cuentas del sistema. Cada prueba se deshace al terminar. */
@SpringBootTest
@Transactional
class UsuarioServiceTest {

    @Autowired private UsuarioService    usuarioService;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private PasswordEncoder   passwordEncoder;

    @Test
    void existenLasDosCuentasDelSistema() {
        Usuario jefe = usuarioRepo.findByUsernameIgnoreCase("jefe").orElseThrow();
        Usuario empleado = usuarioRepo.findByUsernameIgnoreCase("empleado").orElseThrow();
        assertThat(jefe.getRol()).isEqualTo(Rol.JEFE);
        assertThat(empleado.getRol()).isEqualTo(Rol.EMPLEADO);
        assertThat(jefe.getPasswordHash()).startsWith("{bcrypt}");
    }

    @Test
    void cambiaLaContrasenaDefinitivamente() {
        Usuario empleado = usuarioRepo.findByUsernameIgnoreCase("empleado").orElseThrow();

        usuarioService.cambiarPassword(empleado.getId(), "taller2026", "taller2026");

        Usuario actualizado = usuarioRepo.findById(empleado.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("taller2026", actualizado.getPasswordHash())).isTrue();
        assertThat(actualizado.isDebeCambiarPassword()).isFalse();
    }

    @Test
    void rechazaContrasenaCortaOSinConfirmar() {
        Long id = usuarioRepo.findByUsernameIgnoreCase("jefe").orElseThrow().getId();
        assertThatThrownBy(() -> usuarioService.cambiarPassword(id, "123", "123"))
                .isInstanceOf(NegocioException.class).hasMessageContaining("al menos");
        assertThatThrownBy(() -> usuarioService.cambiarPassword(id, "secreta1", "secreta2"))
                .isInstanceOf(NegocioException.class).hasMessageContaining("no coinciden");
    }
}
