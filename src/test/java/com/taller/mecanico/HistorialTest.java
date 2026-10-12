package com.taller.mecanico;

import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.enums.EstadoOrden;
import com.taller.mecanico.enums.VistaHistorial;
import com.taller.mecanico.repository.OrdenIngresoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pestañas por estado del historial. Datos de ejemplo: ORD-001 y ORD-002 EN_PROCESO.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "empleado", roles = "EMPLEADO")
class HistorialTest {

    @Autowired private MockMvc mvc;
    @Autowired private OrdenIngresoRepository ordenRepository;

    @Test
    void sinPestanaMuestraTodasConLosConteos() throws Exception {
        mvc.perform(get("/ordenes/lista"))
           .andExpect(status().isOk())
           .andExpect(model().attribute("vistaActual", VistaHistorial.TODAS))
           .andExpect(content().string(containsString("Por hacer")))
           .andExpect(content().string(containsString("Listas para entregar")))
           .andExpect(content().string(containsString("En el taller")));
    }

    @Test
    void elEnlaceConEstadoAbreLaPestanaEquivalente() throws Exception {
        mvc.perform(get("/ordenes/lista").param("estado", "LISTO"))
           .andExpect(status().isOk())
           .andExpect(model().attribute("vistaActual", VistaHistorial.LISTO));
    }

    @Test
    @Transactional
    void cadaPestanaMuestraSusEstadosYCuentaBien() throws Exception {
        OrdenIngreso orden = ordenRepository.findById("ORD-001").orElseThrow();
        orden.setEstado(EstadoOrden.LISTO);
        ordenRepository.flush();

        assertThat(ids(get("/ordenes/lista").param("vista", "LISTO"))).contains("ORD-001");
        assertThat(ids(get("/ordenes/lista").param("vista", "POR_HACER"))).doesNotContain("ORD-001");

        Map<VistaHistorial, Long> conteo = conteo(get("/ordenes/lista").param("vista", "POR_HACER"));
        assertThat(conteo.get(VistaHistorial.POR_HACER))
                .isEqualTo(conteo.get(VistaHistorial.PENDIENTE) + conteo.get(VistaHistorial.EN_PROCESO));
        assertThat(conteo.get(VistaHistorial.TODAS)).isEqualTo(ordenRepository.count());
    }

    @Test
    @Transactional
    void lasPorHacerVanDeLaMasAntiguaALaMasNueva() throws Exception {
        OrdenIngreso antigua = ordenRepository.findById("ORD-002").orElseThrow();
        antigua.setFecha(LocalDateTime.now().minusYears(5));
        antigua.setEstado(EstadoOrden.PENDIENTE);
        ordenRepository.flush();

        List<String> ids = ids(get("/ordenes/lista").param("vista", "POR_HACER"));
        assertThat(ids).first().isEqualTo("ORD-002");
    }

    @SuppressWarnings("unchecked")
    private List<String> ids(org.springframework.test.web.servlet.RequestBuilder req) throws Exception {
        MvcResult r = mvc.perform(req).andExpect(status().isOk()).andReturn();
        return ((List<OrdenIngreso>) r.getModelAndView().getModel().get("ordenes"))
                .stream().map(OrdenIngreso::getId).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<VistaHistorial, Long> conteo(org.springframework.test.web.servlet.RequestBuilder req) throws Exception {
        MvcResult r = mvc.perform(req).andExpect(status().isOk()).andReturn();
        return (Map<VistaHistorial, Long>) r.getModelAndView().getModel().get("conteoVistas");
    }
}
