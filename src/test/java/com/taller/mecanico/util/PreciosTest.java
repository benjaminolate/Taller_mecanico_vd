package com.taller.mecanico.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PreciosTest {

    @Test
    void calculaIvaRedondeandoAPesos() {
        assertThat(Precios.conIva(new BigDecimal("10000"))).isEqualByComparingTo("11900");
        assertThat(Precios.neto(new BigDecimal("11900"))).isEqualByComparingTo("10000");
        assertThat(Precios.conIva(new BigDecimal("45000"))).isEqualByComparingTo("53550");
        assertThat(Precios.neto(new BigDecimal("50000"))).isEqualByComparingTo("42017");
        assertThat(Precios.conIva(null)).isNull();
    }
}
