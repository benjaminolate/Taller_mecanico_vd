package com.taller.mecanico;

import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.enums.EstadoOrden;
import com.taller.mecanico.repository.OrdenIngresoRepository;
import com.taller.mecanico.service.PanelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Panel de inicio. Datos de ejemplo: ORD-001 y ORD-002 EN_PROCESO,
 * mecánicos Mario Demo y Lucía Ejemplo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PanelTest {

    @Autowired private MockMvc mvc;
    @Autowired private PanelService panelService;
    @Autowired private OrdenIngresoRepository ordenRepository;

    @Test
    @WithMockUser(username = "jefe", roles = "JEFE")
    void elJefeVeElResumenPorMecanico() throws Exception {
        mvc.perform(get("/"))
           .andExpect(status().isOk())
           .andExpect(model().attributeExists("conteo", "listas", "atrasadas", "resumenMecanicos"))
           .andExpect(content().string(containsString("Mecánicos este mes")))
           .andExpect(content().string(containsString("Mario Demo")));
    }

    @Test
    @WithMockUser(username = "jefe", roles = "JEFE")
    void desdeCualquierPantallaSeVuelveAlPanel() throws Exception {
        mvc.perform(get("/ordenes/lista"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("<a class=\"nav-link\" href=\"/\">Inicio</a>")))
           .andExpect(content().string(containsString("<a class=\"navbar-brand fw-bold\" href=\"/\">")));
    }

    @Test
    @WithMockUser(username = "empleado", roles = "EMPLEADO")
    void elEmpleadoVeElPanelSinElResumenPorMecanico() throws Exception {
        mvc.perform(get("/"))
           .andExpect(status().isOk())
           .andExpect(model().attributeDoesNotExist("resumenMecanicos"))
           .andExpect(content().string(containsString("Listas para entregar")))
           .andExpect(content().string(not(containsString("Mecánicos este mes"))));
    }

    @Test
    @Transactional
    void unaOrdenAntiguaSinTerminarApareceComoAtrasada() {
        OrdenIngreso orden = ordenRepository.findById("ORD-001").orElseThrow();
        orden.setFecha(LocalDateTime.now().minusDays(PanelService.DIAS_ATRASO + 3));
        orden.setEstado(EstadoOrden.PENDIENTE);
        ordenRepository.flush();

        assertThat(panelService.atrasadas()).extracting(OrdenIngreso::getId).contains("ORD-001");
        assertThat(panelService.listasParaEntregar()).extracting(OrdenIngreso::getId).doesNotContain("ORD-001");
        assertThat(orden.getDiasDesdeIngreso()).isEqualTo(PanelService.DIAS_ATRASO + 3);
    }

    @Test
    @Transactional
    void unaOrdenListaApareceParaEntregarYNoComoAtrasada() {
        OrdenIngreso orden = ordenRepository.findById("ORD-002").orElseThrow();
        long listasAntes = panelService.conteoPorEstado().get("LISTO");
        orden.setFecha(LocalDateTime.now().minusDays(PanelService.DIAS_ATRASO + 3));
        orden.setEstado(EstadoOrden.LISTO);
        ordenRepository.flush();

        assertThat(panelService.listasParaEntregar()).extracting(OrdenIngreso::getId).contains("ORD-002");
        assertThat(panelService.atrasadas()).extracting(OrdenIngreso::getId).doesNotContain("ORD-002");
        assertThat(panelService.conteoPorEstado().get("LISTO")).isEqualTo(listasAntes + 1);
    }

    @Test
    void elResumenIncluyeALosMecanicosActivos() {
        assertThat(panelService.resumenMecanicosDelMes())
                .extracting(r -> r.nombre())
                .contains("Mario Demo", "Lucía Ejemplo");
    }
}
