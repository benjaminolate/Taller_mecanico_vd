package com.taller.mecanico;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Recorre las pantallas principales con los datos de ejemplo del perfil "dev"
 * (ORD-001 = vehículo, ORD-002 = cremallera) y verifica que se rendericen.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "jefe", roles = "JEFE")
class PantallasTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void inicio() throws Exception {
        mvc.perform(get("/"))
           .andExpect(status().isOk())
           .andExpect(view().name("index"));
    }

    @Test
    void listaDeOrdenesConYSinFiltros() throws Exception {
        mvc.perform(get("/ordenes/lista")).andExpect(status().isOk());
        mvc.perform(get("/ordenes/lista")
                .param("patente", "DEMO")
                .param("estado", "EN_PROCESO")
                .param("mecanicoId", "1")
                .param("clienteRut", "44444444")
                .param("fechaDesde", "2020-01-01"))
           .andExpect(status().isOk());
    }

    @Test
    void formularioNuevaOrden() throws Exception {
        mvc.perform(get("/ordenes/nueva")).andExpect(status().isOk());
    }

    @Test
    void detalleDeOrdenes() throws Exception {
        mvc.perform(get("/ordenes/ORD-001")).andExpect(status().isOk());
        mvc.perform(get("/ordenes/ORD-002")).andExpect(status().isOk());
    }

    @Test
    void gestionDeTrabajos() throws Exception {
        mvc.perform(get("/ordenes/ORD-001/trabajos")).andExpect(status().isOk());
        mvc.perform(get("/ordenes/ORD-002/trabajos")).andExpect(status().isOk());
    }

    @Test
    void pdfDeOrdenes() throws Exception {
        mvc.perform(get("/ordenes/ORD-001/pdf"))
           .andExpect(status().isOk())
           .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        mvc.perform(get("/ordenes/ORD-002/pdf"))
           .andExpect(status().isOk())
           .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void autocompletadoDeVehiculoNormalizaLaPatente() throws Exception {
        mvc.perform(get("/ordenes/api/vehiculos/patente/demo-01"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.patente").value("DEMO01"))
           .andExpect(jsonPath("$.marca").value("Toyota"));
        mvc.perform(get("/ordenes/api/vehiculos/patente/ZZZZ99")).andExpect(status().isNotFound());
    }

    @Test
    void autocompletadoDeClienteDevuelveSoloLosDatosNecesarios() throws Exception {
        mvc.perform(get("/ordenes/api/clientes/1"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.nombre").value("Andrea Muestra"))
           .andExpect(jsonPath("$.rut").value("44.444.444-4"))
           .andExpect(jsonPath("$.ordenes").doesNotExist());
    }

    @Test
    @Transactional
    void guardarOrdenConPatenteEnMinusculasDeUnVehiculoExistente() throws Exception {
        mvc.perform(post("/ordenes/guardar").with(csrf())
                .param("clienteNombre", "Andrea Muestra")
                .param("recibidoPorId", "1")
                .param("clienteRut", "44444444-4")
                .param("tipoIngreso", "VEHICULO")
                .param("vehiculoPatente", "demo 01"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrlPattern("/ordenes/ORD-*/confirmada"));
    }

    @Test
    @Transactional
    void guardarOrdenConRutInvalidoMuestraElError() throws Exception {
        mvc.perform(post("/ordenes/guardar").with(csrf())
                .param("clienteNombre", "Cliente Nuevo")
                .param("recibidoPorId", "1")
                .param("clienteRut", "44.444.444-0")
                .param("tipoIngreso", "CREMALLERA"))
           .andExpect(status().isOk())
           .andExpect(view().name("ordenes/nueva-orden"))
           .andExpect(model().attribute("error", containsString("no es válido")));
    }

    @Test
    void cambioDeEstadoNoPermitidoMuestraMensaje() throws Exception {
        // ORD-001 está EN_PROCESO y tiene un trabajo pendiente: no puede pasar a ENTREGADO
        mvc.perform(post("/ordenes/ORD-001/trabajos/cambiar-estado").with(csrf()).param("nuevoEstado", "ENTREGADO"))
           .andExpect(status().is3xxRedirection())
           .andExpect(flash().attribute("mensajeError", containsString("No se puede pasar")));
    }

    @Test
    @Transactional
    void guardarOrdenSinQuienRecibeMarcaElCampo() throws Exception {
        mvc.perform(post("/ordenes/guardar").with(csrf())
                .param("clienteNombre", "Cliente Nuevo")
                .param("tipoIngreso", "CREMALLERA"))
           .andExpect(status().isOk())
           .andExpect(view().name("ordenes/nueva-orden"))
           .andExpect(model().attributeHasFieldErrors("ordenRequest", "recibidoPorId"));
    }

    @Test
    void bootstrapSeSirveDesdeLaApp() throws Exception {
        mvc.perform(get("/vendor/bootstrap/bootstrap.bundle.min.js")).andExpect(status().isOk());
        mvc.perform(get("/")).andExpect(content().string(containsString("/vendor/bootstrap/bootstrap.bundle.min.js")))
           .andExpect(content().string(containsString("Cerrar sesión")));
    }

    @Test
    void recursoInexistenteDa404() throws Exception {
        mvc.perform(get("/favicon-que-no-existe.ico")).andExpect(status().isNotFound());
    }

    @Test
    void ordenInexistenteDa404() throws Exception {
        mvc.perform(get("/ordenes/ORD-999999")).andExpect(status().isNotFound());
    }
}
