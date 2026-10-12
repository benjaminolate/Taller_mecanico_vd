package com.taller.mecanico;

import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.enums.EstadoOrden;
import com.taller.mecanico.repository.OrdenIngresoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Aviso por WhatsApp en el detalle de una orden lista. */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "empleado", roles = "EMPLEADO")
class WhatsappTest {

    @Autowired private MockMvc mvc;
    @Autowired private OrdenIngresoRepository ordenRepository;

    @Test
    @Transactional
    void ordenListaAbreElChatDirectoConElMensaje() throws Exception {
        OrdenIngreso orden = ordenRepository.findById("ORD-001").orElseThrow();
        orden.setEstado(EstadoOrden.LISTO);
        orden.getCliente().setTelefono("9 0000 0001");
        ordenRepository.flush();

        mvc.perform(get("/ordenes/ORD-001"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("href=\"whatsapp://send?phone=56900000001&amp;text=Hola")))
           .andExpect(content().string(containsString("href=\"https://web.whatsapp.com/send?phone=56900000001&amp;text=")))
           .andExpect(content().string(containsString("target=\"whatsapp-web\"")))
           .andExpect(content().string(not(containsString("wa.me"))));
    }

    @Test
    @Transactional
    void sinTelefonoValidoNoMuestraElAviso() throws Exception {
        OrdenIngreso orden = ordenRepository.findById("ORD-001").orElseThrow();
        orden.setEstado(EstadoOrden.LISTO);
        orden.getCliente().setTelefono("123");
        ordenRepository.flush();

        mvc.perform(get("/ordenes/ORD-001"))
           .andExpect(status().isOk())
           .andExpect(content().string(not(containsString("id=\"whatsapp-app\""))));
    }
}
