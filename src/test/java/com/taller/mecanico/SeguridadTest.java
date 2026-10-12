package com.taller.mecanico;

import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.repository.OrdenIngresoRepository;
import com.taller.mecanico.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login y permisos por rol. Usa los usuarios del perfil dev:
 * jefe / jefedemo y empleado / empleadodemo.
 */
@SpringBootTest(properties = "taller.password-respaldo-hash={noop}respaldo-demo")
@AutoConfigureMockMvc
class SeguridadTest {

    @Autowired private MockMvc                mvc;
    @Autowired private UsuarioService         usuarioService;
    @Autowired private OrdenIngresoRepository ordenRepo;

    // ── Login ────────────────────────────────────────────────────────────────

    @Test
    void sinSesionSeRedirigeAlLogin() throws Exception {
        mvc.perform(get("/ordenes/lista"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void loginCorrectoEIncorrecto() throws Exception {
        mvc.perform(formLogin("/login").user("jefe").password("jefedemo"))
           .andExpect(authenticated().withUsername("jefe").withRoles("JEFE"));
        mvc.perform(formLogin("/login").user("JEFE").password("jefedemo"))
           .andExpect(authenticated().withUsername("jefe"));
        mvc.perform(formLogin("/login").user("empleado").password("empleadodemo"))
           .andExpect(authenticated().withRoles("EMPLEADO"));
        mvc.perform(formLogin("/login").user("jefe").password("incorrecta"))
           .andExpect(unauthenticated())
           .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void formulariosSinTokenCsrfSonRechazados() throws Exception {
        mvc.perform(post("/ordenes/guardar").with(user("jefe").roles("JEFE")))
           .andExpect(status().isForbidden());
    }

    // ── Permisos del empleado ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "empleado", roles = "EMPLEADO")
    void empleadoUsaLasPantallasDeTrabajoDiario() throws Exception {
        mvc.perform(get("/ordenes/lista")).andExpect(status().isOk());
        mvc.perform(get("/ordenes/nueva")).andExpect(status().isOk());
        mvc.perform(get("/ordenes/ORD-001")).andExpect(status().isOk());
        mvc.perform(get("/ordenes/ORD-001/pdf")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "empleado", roles = "EMPLEADO")
    void empleadoNoVeNiUsaLasAccionesDelJefe() throws Exception {
        mvc.perform(get("/ordenes/ORD-001/trabajos"))
           .andExpect(status().isOk())
           .andExpect(content().string(not(containsString("/trabajos/cambiar-estado"))))
           .andExpect(content().string(not(containsString("/eliminar"))))
           .andExpect(content().string(not(containsString("href=\"/usuarios\""))));

        mvc.perform(post("/ordenes/ORD-001/trabajos/cambiar-estado").with(csrf()).param("nuevoEstado", "LISTO"))
           .andExpect(status().isForbidden());
        mvc.perform(post("/ordenes/ORD-001/trabajos/1/eliminar").with(csrf()))
           .andExpect(status().isForbidden());
        mvc.perform(get("/usuarios")).andExpect(status().isForbidden());
        mvc.perform(get("/usuarios/nuevo")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "jefe", roles = "JEFE")
    void jefeVeLasAccionesAdministrativas() throws Exception {
        mvc.perform(get("/ordenes/ORD-001/trabajos"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("/trabajos/cambiar-estado")))
           .andExpect(content().string(containsString("/eliminar")));
        mvc.perform(get("/usuarios")).andExpect(status().isOk())
           .andExpect(content().string(containsString("empleado")))
           .andExpect(content().string(containsString("Cambiar contraseña")));
    }

    // ── Contraseña de respaldo y cambio de contraseñas por el jefe ──────────

    @Test
    void laContrasenaDeRespaldoAbreCualquierCuenta() throws Exception {
        mvc.perform(formLogin("/login").user("jefe").password("respaldo-demo"))
           .andExpect(authenticated().withUsername("jefe").withRoles("JEFE"));
        mvc.perform(formLogin("/login").user("empleado").password("respaldo-demo"))
           .andExpect(authenticated().withUsername("empleado").withRoles("EMPLEADO"));
        mvc.perform(formLogin("/login").user("noexiste").password("respaldo-demo"))
           .andExpect(unauthenticated());
    }

    @Test
    @Transactional
    void jefeCambiaLaContrasenaDelEmpleadoDefinitivamente() throws Exception {
        Long idEmpleado = usuarioService.buscarPorUsername("empleado").orElseThrow().getId();

        mvc.perform(post("/usuarios/" + idEmpleado + "/password").with(csrf()).with(user("jefe").roles("JEFE"))
                .param("nueva", "nueva2026").param("confirmacion", "otra"))
           .andExpect(redirectedUrl("/usuarios"))
           .andExpect(flash().attribute("mensajeError", containsString("no coinciden")));

        mvc.perform(post("/usuarios/" + idEmpleado + "/password").with(csrf()).with(user("jefe").roles("JEFE"))
                .param("nueva", "nueva2026").param("confirmacion", "nueva2026"))
           .andExpect(redirectedUrl("/usuarios"))
           .andExpect(flash().attribute("mensajeExito", containsString("empleado")));

        mvc.perform(formLogin("/login").user("empleado").password("nueva2026"))
           .andExpect(authenticated().withUsername("empleado"));
        mvc.perform(formLogin("/login").user("empleado").password("empleadodemo"))
           .andExpect(unauthenticated());
    }

    // ── Auditoría ───────────────────────────────────────────────────────────

    @Test
    @Transactional
    @WithMockUser(username = "empleado", roles = "EMPLEADO")
    void laOrdenRegistraQuienLaCreo() throws Exception {
        MvcResult res = mvc.perform(post("/ordenes/guardar").with(csrf())
                .param("clienteNombre", "Cliente Auditoría")
                .param("recibidoPorId", "1")
                .param("tipoIngreso", "CREMALLERA"))
           .andExpect(status().is3xxRedirection())
           .andReturn();

        String id = res.getResponse().getRedirectedUrl()
                .replace("/ordenes/", "").replace("/confirmada", "");
        OrdenIngreso orden = ordenRepo.findById(id).orElseThrow();
        assertThat(orden.getCreadoPor()).isEqualTo("empleado");
        assertThat(orden.getCreadoEn()).isNotNull();
    }
}
