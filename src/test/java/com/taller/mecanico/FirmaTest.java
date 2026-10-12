package com.taller.mecanico;

import com.taller.mecanico.entity.FirmaOrden;
import com.taller.mecanico.enums.EstadoOrden;
import com.taller.mecanico.enums.MomentoFirma;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.repository.FirmaOrdenRepository;
import com.taller.mecanico.service.FirmaService;
import com.taller.mecanico.service.TrabajoRealizadoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Firmas del cliente. Datos de ejemplo: ORD-001 (vehículo) y ORD-002 (cremallera),
 * ambas EN_PROCESO y sin firmas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "empleado", roles = "EMPLEADO")
class FirmaTest {

    /** PNG real de 4x2 píxeles. */
    static final String PNG = "data:image/png;base64,"
            + "iVBORw0KGgoAAAANSUhEUgAAAAQAAAACCAYAAAB/qH1jAAAAEElEQVR4nGNgYGD4j4ZRBQB7pgf5fzpslgAAAABJRU5ErkJggg==";

    @Autowired private MockMvc                 mvc;
    @Autowired private FirmaService            firmaService;
    @Autowired private FirmaOrdenRepository    firmaRepo;
    @Autowired private TrabajoRealizadoService trabajoService;

    @Test
    void pantallasDeFirma() throws Exception {
        mvc.perform(get("/firmas")).andExpect(status().isOk())
           .andExpect(content().string(containsString("ORD-001")));
        mvc.perform(get("/ordenes/ORD-001/firmar/recepcion").param("volver", "/firmas"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("Firme aquí")));
        mvc.perform(get("/ordenes/ORD-002/firmar/recepcion")).andExpect(status().isOk());
        // ORD-001 no está lista: la firma de retiro no corresponde todavía
        mvc.perform(get("/ordenes/ORD-001/firmar/retiro")).andExpect(status().isBadRequest());
        mvc.perform(get("/ordenes/ORD-001/firmar/otra-cosa")).andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void firmaDeRecepcionSeGuardaYApareceEnDetalleYPdf() throws Exception {
        mvc.perform(post("/ordenes/ORD-001/firmar/recepcion").with(csrf())
                .param("accion", "firmar").param("imagen", PNG)
                .param("nombre", "Andrea Muestra").param("rut", "44444444-4").param("volver", "/firmas"))
           .andExpect(redirectedUrl("/firmas"));

        FirmaOrden f = firmaRepo.findByOrdenIdAndMomentoAndAnuladaFalse("ORD-001", MomentoFirma.RECEPCION).orElseThrow();
        assertThat(f.isFirmada()).isTrue();
        assertThat(f.getImagenPng()).isNotEmpty();
        assertThat(f.getRutFirmante()).isEqualTo("44.444.444-4");
        assertThat(f.getCreadoPor()).isEqualTo("empleado");

        mvc.perform(get("/ordenes/ORD-001"))
           .andExpect(content().string(containsString("data:image/png;base64,")));
        mvc.perform(get("/ordenes/ORD-001/pdf"))
           .andExpect(status().isOk())
           .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        // Ya no aparece como pendiente
        mvc.perform(get("/firmas")).andExpect(content().string(
                org.hamcrest.Matchers.not(containsString("/ordenes/ORD-001/firmar/recepcion"))));
    }

    @Test
    @Transactional
    void continuarSinFirmaDejaRegistro() throws Exception {
        mvc.perform(post("/ordenes/ORD-002/firmar/recepcion").with(csrf()).param("accion", "sin-firma"))
           .andExpect(redirectedUrl("/ordenes/ORD-002"));

        FirmaOrden f = firmaRepo.findByOrdenIdAndMomentoAndAnuladaFalse("ORD-002", MomentoFirma.RECEPCION).orElseThrow();
        assertThat(f.isFirmada()).isFalse();
        assertThat(f.getImagenPng()).isNull();
        mvc.perform(get("/ordenes/ORD-002/pdf")).andExpect(status().isOk());
    }

    @Test
    @Transactional
    void firmarSinTrazoMuestraElAviso() throws Exception {
        mvc.perform(post("/ordenes/ORD-001/firmar/recepcion").with(csrf())
                .param("accion", "firmar").param("nombre", "Andrea"))
           .andExpect(status().isOk())
           .andExpect(model().attribute("error", containsString("Continuar sin firma")));
    }

    @Test
    @Transactional
    void unaFirmaNoSeRepiteSinAnularla() {
        firmaService.registrar("ORD-001", MomentoFirma.RECEPCION, PNG, "Andrea", null);

        assertThatThrownBy(() -> firmaService.registrar("ORD-001", MomentoFirma.RECEPCION, null, null, null))
                .isInstanceOf(NegocioException.class).hasMessageContaining("anúlala");

        FirmaOrden vigente = firmaRepo.findByOrdenIdAndMomentoAndAnuladaFalse("ORD-001", MomentoFirma.RECEPCION).orElseThrow();
        firmaService.anular(vigente.getId());
        firmaService.registrar("ORD-001", MomentoFirma.RECEPCION, PNG, "Andrea Muestra", null);

        assertThat(firmaRepo.findByOrdenIdOrderByIdDesc("ORD-001")).hasSize(2);
        assertThat(firmaService.vigentes("ORD-001").get(MomentoFirma.RECEPCION).getNombreFirmante()).isEqualTo("Andrea Muestra");
    }

    @Test
    @Transactional
    void rechazaImagenesInvalidasYRutInvalido() {
        assertThatThrownBy(() -> firmaService.registrar("ORD-001", MomentoFirma.RECEPCION,
                "data:image/png;base64,bm8tZXMtcG5n", "Andrea", null))
                .isInstanceOf(NegocioException.class).hasMessageContaining("no es válida");
        assertThatThrownBy(() -> firmaService.registrar("ORD-001", MomentoFirma.RECEPCION,
                "data:image/jpeg;base64,AAAA", "Andrea", null))
                .isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> firmaService.registrar("ORD-001", MomentoFirma.RECEPCION, PNG, " ", null))
                .isInstanceOf(NegocioException.class).hasMessageContaining("nombre");
        assertThatThrownBy(() -> firmaService.registrar("ORD-001", MomentoFirma.RECEPCION, PNG, "Andrea", "44.444.444-0"))
                .isInstanceOf(NegocioException.class).hasMessageContaining("RUT");
    }

    @Test
    @Transactional
    void firmaDeRetiroCuandoLaOrdenEstaLista() throws Exception {
        // ORD-002 tiene un único trabajo pendiente (id 3): al completarlo queda LISTA
        trabajoService.marcarCompletado("ORD-002", 3L);
        mvc.perform(get("/firmas")).andExpect(content().string(containsString("/ordenes/ORD-002/firmar/retiro")));
        mvc.perform(get("/ordenes/ORD-002/firmar/retiro")).andExpect(status().isOk())
           .andExpect(content().string(containsString("Trabajos realizados")));

        firmaService.registrar("ORD-002", MomentoFirma.RETIRO, PNG, "Bruno Ejemplo", null);
        trabajoService.cambiarEstadoOrden("ORD-002", EstadoOrden.ENTREGADO);

        // Entregada: la recepción ya no se puede firmar
        assertThatThrownBy(() -> firmaService.registrar("ORD-002", MomentoFirma.RECEPCION, null, null, null))
                .isInstanceOf(NegocioException.class).hasMessageContaining("entregada");
    }
}
