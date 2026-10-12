package com.taller.mecanico;

import com.taller.mecanico.config.UsuarioInicial;
import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.repository.OrdenIngresoRepository;
import com.taller.mecanico.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quién recibe y quién realiza el trabajo, y cambio de nombre de usuario.
 * Datos de ejemplo: ORD-001 y ORD-002 recibidas por Lucía Ejemplo (2) y asignadas a Mario Demo (1).
 */
@SpringBootTest
@AutoConfigureMockMvc
class RecepcionYCuentasTest {

    @Autowired private MockMvc mvc;
    @Autowired private OrdenIngresoRepository ordenRepo;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private UsuarioInicial usuarioInicial;
    @Autowired private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // ── Recibe / realiza ────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "empleado", roles = "EMPLEADO")
    void laNuevaOrdenSoloPideQuienRecibe() throws Exception {
        mvc.perform(get("/ordenes/nueva"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("¿Quién recibe el vehículo?")))
           .andExpect(content().string(containsString("name=\"recibidoPorId\"")))
           .andExpect(content().string(not(containsString("name=\"mecanicoId\""))));
    }

    @Test
    @Transactional
    @WithMockUser(username = "empleado", roles = "EMPLEADO")
    void elEmpleadoAsignaElMecanicoDesdeElHistorialYVuelveALaMismaPestana() throws Exception {
        OrdenIngreso orden = ordenRepo.findById("ORD-002").orElseThrow();
        orden.setMecanico(null);
        ordenRepo.flush();

        mvc.perform(get("/ordenes/lista").param("vista", "POR_HACER"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("action=\"/ordenes/ORD-002/mecanico\"")))
           .andExpect(content().string(containsString("Recibió: Lucía Ejemplo")));
        mvc.perform(get("/"))
           .andExpect(content().string(containsString("id=\"sin-mecanico\"")));

        mvc.perform(post("/ordenes/ORD-002/mecanico").with(csrf())
                .param("mecanicoId", "2")
                .param("volver", "/ordenes/lista?vista=POR_HACER"))
           .andExpect(redirectedUrl("/ordenes/lista?vista=POR_HACER"))
           .andExpect(flash().attribute("mensajeExito", containsString("Lucía Ejemplo")));

        assertThat(ordenRepo.findById("ORD-002").orElseThrow().getMecanico().getId()).isEqualTo(2L);
    }

    @Test
    @Transactional
    @WithMockUser(username = "empleado", roles = "EMPLEADO")
    void elDetalleMuestraQuienRecibioYNoRedirigeAFuera() throws Exception {
        mvc.perform(get("/ordenes/ORD-001"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Recibido por")))
           .andExpect(content().string(containsString("id=\"form-asignar-mecanico\"")));

        mvc.perform(post("/ordenes/ORD-001/mecanico").with(csrf())
                .param("mecanicoId", "2")
                .param("volver", "https://otro-sitio.com"))
           .andExpect(redirectedUrl("/ordenes/ORD-001"));
    }

    // ── Nombre de usuario ───────────────────────────────────────────────────

    @Test
    @Transactional
    @WithMockUser(username = "jefe", roles = "JEFE")
    void elJefeCambiaElUsuarioDelEmpleadoYSeEntraConElNuevo() throws Exception {
        Long id = usuarioRepo.findByUsernameIgnoreCase("empleado").orElseThrow().getId();

        mvc.perform(post("/usuarios/" + id + "/username").with(csrf()).param("nuevo", " Taller.Trabajadores "))
           .andExpect(redirectedUrl("/usuarios"))
           .andExpect(flash().attribute("mensajeExito", containsString("taller.trabajadores")));

        assertThat(usuarioRepo.findByUsernameIgnoreCase("empleado")).isEmpty();
        // El login encuentra la cuenta con el nombre nuevo y la misma contraseña
        var cuenta = userDetailsService.loadUserByUsername("taller.trabajadores");
        assertThat(passwordEncoder.matches("empleadodemo", cuenta.getPassword())).isTrue();

        // Al reiniciar la app no se vuelve a crear una cuenta "empleado"
        usuarioInicial.run(null);
        assertThat(usuarioRepo.findByUsernameIgnoreCase("empleado")).isEmpty();
    }

    @Test
    @Transactional
    @WithMockUser(username = "jefe", roles = "JEFE")
    void noAceptaUsuariosRepetidosNiConEspacios() throws Exception {
        Long id = usuarioRepo.findByUsernameIgnoreCase("empleado").orElseThrow().getId();

        mvc.perform(post("/usuarios/" + id + "/username").with(csrf()).param("nuevo", "JEFE"))
           .andExpect(flash().attribute("mensajeError", containsString("Ya existe")));
        mvc.perform(post("/usuarios/" + id + "/username").with(csrf()).param("nuevo", "con espacio"))
           .andExpect(flash().attribute("mensajeError", containsString("entre 3 y 30")));
        assertThat(usuarioRepo.findById(id).orElseThrow().getUsername()).isEqualTo("empleado");
    }

    @Test
    @Transactional
    void siElJefeCambiaSuPropioUsuarioDebeEntrarDeNuevo() throws Exception {
        Long id = usuarioRepo.findByUsernameIgnoreCase("jefe").orElseThrow().getId();

        mvc.perform(post("/usuarios/" + id + "/username").with(csrf()).with(user("jefe").roles("JEFE"))
                .param("nuevo", "papa"))
           .andExpect(redirectedUrl("/login?usuarioCambiado"));
        assertThat(usuarioRepo.findById(id).orElseThrow().getUsername()).isEqualTo("papa");
    }

    @Test
    @WithMockUser(username = "empleado", roles = "EMPLEADO")
    void elEmpleadoNoPuedeCambiarUsuarios() throws Exception {
        mvc.perform(post("/usuarios/1/username").with(csrf()).param("nuevo", "otro"))
           .andExpect(status().isForbidden());
    }
}
