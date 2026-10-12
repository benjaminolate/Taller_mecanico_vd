package com.taller.mecanico;

import com.taller.mecanico.entity.CatalogoTrabajo;
import com.taller.mecanico.repository.CatalogoTrabajoRepository;
import com.taller.mecanico.repository.ClienteRepository;
import com.taller.mecanico.repository.OrdenIngresoRepository;
import com.taller.mecanico.repository.TrabajoRealizadoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Mecánicos, catálogo, clientes y edición de órdenes y trabajos. */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "jefe", roles = "JEFE")
class ModulosTest {

    @Autowired private MockMvc                    mvc;
    @Autowired private CatalogoTrabajoRepository  catalogoRepo;
    @Autowired private ClienteRepository          clienteRepo;
    @Autowired private OrdenIngresoRepository     ordenRepo;
    @Autowired private TrabajoRealizadoRepository trabajoRepo;

    // ── Mecánicos ────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void jefeAdministraMecanicos() throws Exception {
        mvc.perform(get("/mecanicos")).andExpect(status().isOk())
           .andExpect(content().string(containsString("Mario Demo")));
        mvc.perform(get("/mecanicos/nuevo")).andExpect(status().isOk());
        mvc.perform(get("/mecanicos/1/editar")).andExpect(status().isOk());

        mvc.perform(post("/mecanicos/guardar").with(csrf())
                .param("nombre", "Pablo Ficticio").param("rut", "10000013-k").param("activo", "true"))
           .andExpect(redirectedUrl("/mecanicos"));

        // RUT repetido (Mario Demo tiene 11.111.111-1)
        mvc.perform(post("/mecanicos/guardar").with(csrf())
                .param("nombre", "Otro").param("rut", "11111111-1").param("activo", "true"))
           .andExpect(status().isOk())
           .andExpect(view().name("mecanicos/form-mecanico"))
           .andExpect(model().attribute("error", containsString("ya está asignado")));
    }

    // ── Catálogo ─────────────────────────────────────────────────────────────

    @Test
    @Transactional
    void catalogoGuardaPrecioConIvaYCalculaElNeto() throws Exception {
        mvc.perform(get("/catalogo")).andExpect(status().isOk());
        mvc.perform(post("/catalogo/guardar").with(csrf())
                .param("nombreTarea", "Alineación")
                .param("precio", "11900")
                .param("precioIncluyeIva", "true")
                .param("activo", "true"))
           .andExpect(redirectedUrl("/catalogo"));

        CatalogoTrabajo c = catalogoRepo.findAll().stream()
                .filter(t -> t.getNombreTarea().equals("Alineación")).findFirst().orElseThrow();
        assertThat(c.getPrecioNeto()).isEqualByComparingTo("10000");
        assertThat(c.getPrecioConIva()).isEqualByComparingTo("11900");
    }

    @Test
    @Transactional
    void tareaInactivaNoApareceAlRegistrarTrabajos() throws Exception {
        CatalogoTrabajo c = catalogoRepo.findAll().stream()
                .min(Comparator.comparing(CatalogoTrabajo::getId)).orElseThrow();
        mvc.perform(post("/catalogo/guardar").with(csrf())
                .param("id", c.getId().toString())
                .param("nombreTarea", "Tarea retirada")
                .param("precio", "1000"))
           .andExpect(redirectedUrl("/catalogo"));

        mvc.perform(get("/ordenes/ORD-001/trabajos"))
           .andExpect(content().string(not(containsString("Tarea retirada"))));
    }

    @Test
    @WithMockUser(username = "empleado", roles = "EMPLEADO")
    void empleadoNoAdministraMecanicosNiCatalogo() throws Exception {
        mvc.perform(get("/mecanicos")).andExpect(status().isForbidden());
        mvc.perform(get("/catalogo")).andExpect(status().isForbidden());
        mvc.perform(post("/catalogo/guardar").with(csrf()).param("nombreTarea", "X"))
           .andExpect(status().isForbidden());
        mvc.perform(get("/clientes")).andExpect(status().isOk());
    }

    // ── Clientes ─────────────────────────────────────────────────────────────

    @Test
    void buscaClientesPorNombreTelefonoYRut() throws Exception {
        mvc.perform(get("/clientes")).andExpect(status().isOk())
           .andExpect(content().string(containsString("Andrea Muestra")))
           .andExpect(content().string(containsString("Bruno Ejemplo")));
        mvc.perform(get("/clientes").param("q", "andrea"))
           .andExpect(content().string(containsString("Andrea Muestra")))
           .andExpect(content().string(not(containsString("Bruno Ejemplo"))));
        mvc.perform(get("/clientes").param("q", "44444444"))
           .andExpect(content().string(containsString("Andrea Muestra")));
        mvc.perform(get("/clientes").param("q", "0002"))
           .andExpect(content().string(containsString("Bruno Ejemplo")));
        mvc.perform(get("/clientes").param("q", "nadie-asi"))
           .andExpect(content().string(containsString("No se encontraron clientes")));
    }

    @Test
    void historialDelCliente() throws Exception {
        mvc.perform(get("/clientes/1")).andExpect(status().isOk())
           .andExpect(content().string(containsString("ORD-001")));
    }

    @Test
    @Transactional
    void editaClienteValidandoElRut() throws Exception {
        mvc.perform(get("/clientes/1/editar")).andExpect(status().isOk());
        mvc.perform(post("/clientes/1/editar").with(csrf())
                .param("nombre", "Andrea Muestra").param("rut", "44.444.444-0"))
           .andExpect(status().isOk())
           .andExpect(model().attribute("error", containsString("no es válido")));
        mvc.perform(post("/clientes/1/editar").with(csrf())
                .param("nombre", "Andrea Muestra Soto").param("rut", "444444444").param("telefono", "+56999999999"))
           .andExpect(redirectedUrl("/clientes/1"));

        assertThat(clienteRepo.findById(1L).orElseThrow().getNombre()).isEqualTo("Andrea Muestra Soto");
        assertThat(clienteRepo.findById(1L).orElseThrow().getRut()).isEqualTo("44.444.444-4");
    }

    // ── Editar orden y trabajo ──────────────────────────────────────────────

    @Test
    @Transactional
    void editaOrdenDeVehiculo() throws Exception {
        mvc.perform(get("/ordenes/ORD-001/editar")).andExpect(status().isOk())
           .andExpect(content().string(containsString("DEMO01")));
        mvc.perform(get("/ordenes/ORD-002/editar")).andExpect(status().isOk());

        mvc.perform(post("/ordenes/ORD-001/editar").with(csrf())
                .param("clienteId", "1")
                .param("recibidoPorId", "1")
                .param("mecanicoId", "2")
                .param("vehiculoPatente", "demo01")
                .param("vehiculoMarca", "Toyota")
                .param("vehiculoModelo", "Yaris Sport")
                .param("golpes", "true")
                .param("nivelCombustible", "40"))
           .andExpect(redirectedUrl("/ordenes/ORD-001"));

        var orden = ordenRepo.findById("ORD-001").orElseThrow();
        assertThat(orden.getVehiculo().getModelo()).isEqualTo("Yaris Sport");
        assertThat(orden.getMecanico().getId()).isEqualTo(2L);
        assertThat(orden.getRecibidoPor().getId()).isEqualTo(1L);
        assertThat(orden.getEstadoVehiculo().getGolpes()).isTrue();
        assertThat(orden.getEstadoVehiculo().getNivelCombustible()).isEqualTo(40);
    }

    @Test
    @Transactional
    void editaTrabajoYProtegeTrabajosDeOtraOrden() throws Exception {
        mvc.perform(get("/ordenes/ORD-001/trabajos/1/editar")).andExpect(status().isOk());
        mvc.perform(post("/ordenes/ORD-001/trabajos/1/editar").with(csrf())
                .param("descripcion", "Revisión completa de dirección")
                .param("categoria", "DIAGNOSTICO")
                .param("costoEstimado", "30000")
                .param("mecanicoId", "2"))
           .andExpect(redirectedUrl("/ordenes/ORD-001/trabajos"));

        var t = trabajoRepo.findById(1L).orElseThrow();
        assertThat(t.getDescripcion()).isEqualTo("Revisión completa de dirección");
        assertThat(t.getCostoEstimado()).isEqualByComparingTo("30000");
        assertThat(t.getMecanico().getId()).isEqualTo(2L);

        // El trabajo 1 es de ORD-001: pedirlo desde ORD-002 no debe funcionar
        mvc.perform(get("/ordenes/ORD-002/trabajos/1/editar")).andExpect(status().isNotFound());
    }
}
