package com.taller.mecanico.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoOrdenTest {

    @Test
    void transicionesPermitidas() {
        assertThat(EstadoOrden.PENDIENTE.puedeCambiarA(EstadoOrden.EN_PROCESO)).isTrue();
        assertThat(EstadoOrden.EN_PROCESO.puedeCambiarA(EstadoOrden.LISTO)).isTrue();
        assertThat(EstadoOrden.EN_PROCESO.puedeCambiarA(EstadoOrden.PENDIENTE)).isTrue();
        assertThat(EstadoOrden.LISTO.puedeCambiarA(EstadoOrden.ENTREGADO)).isTrue();
        assertThat(EstadoOrden.LISTO.puedeCambiarA(EstadoOrden.EN_PROCESO)).isTrue();
    }

    @Test
    void transicionesNoPermitidas() {
        assertThat(EstadoOrden.PENDIENTE.puedeCambiarA(EstadoOrden.ENTREGADO)).isFalse();
        assertThat(EstadoOrden.PENDIENTE.puedeCambiarA(EstadoOrden.LISTO)).isFalse();
        assertThat(EstadoOrden.EN_PROCESO.puedeCambiarA(EstadoOrden.ENTREGADO)).isFalse();
        assertThat(EstadoOrden.ENTREGADO.getSiguientesPermitidos()).isEmpty();
        assertThat(EstadoOrden.ENTREGADO.esFinal()).isTrue();
    }
}
