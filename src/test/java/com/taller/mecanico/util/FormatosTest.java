package com.taller.mecanico.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormatosTest {

    @Test
    void normalizaPatentes() {
        assertThat(Formatos.normalizarPatente("demo-01")).isEqualTo("DEMO01");
        assertThat(Formatos.normalizarPatente(" de mo 01 ")).isEqualTo("DEMO01");
        assertThat(Formatos.normalizarPatente("AB.12.34")).isEqualTo("AB1234");
        assertThat(Formatos.normalizarPatente("   ")).isNull();
        assertThat(Formatos.normalizarPatente(null)).isNull();
    }

    @Test
    void validaPatentes() {
        assertThat(Formatos.patenteValida("DEMO01")).isTrue();   // formato nuevo
        assertThat(Formatos.patenteValida("AB1234")).isTrue();   // formato antiguo
        assertThat(Formatos.patenteValida("ABC12")).isTrue();    // moto
        assertThat(Formatos.patenteValida("ABCDEF")).isFalse();  // sin números
        assertThat(Formatos.patenteValida("123456")).isFalse();  // sin letras
        assertThat(Formatos.patenteValida("AB1")).isFalse();     // muy corta
        assertThat(Formatos.patenteValida("ABCDEF123")).isFalse(); // muy larga
    }

    @Test
    void validaRutConDistintosFormatos() {
        assertThat(Formatos.rutValido("12.345.678-5")).isTrue();
        assertThat(Formatos.rutValido("12345678-5")).isTrue();
        assertThat(Formatos.rutValido("123456785")).isTrue();
        assertThat(Formatos.rutValido("9.876.543-3")).isTrue();
        assertThat(Formatos.rutValido("11.111.111-1")).isTrue();
        assertThat(Formatos.rutValido("12.345.678-0")).isFalse();
        assertThat(Formatos.rutValido("abc")).isFalse();
        assertThat(Formatos.rutValido("")).isFalse();
        assertThat(Formatos.rutValido(null)).isFalse();
    }

    @Test
    void aceptaDigitoVerificadorK() {
        // 10.000.013-K: el dígito verificador calculado es K
        assertThat(Formatos.calcularDv("10000013")).isEqualTo('K');
        assertThat(Formatos.rutValido("10.000.013-k")).isTrue();
        assertThat(Formatos.formatearRut("10000013k")).isEqualTo("10.000.013-K");
    }

    @Test
    void formateaRut() {
        assertThat(Formatos.formatearRut("123456785")).isEqualTo("12.345.678-5");
        assertThat(Formatos.formatearRut("9876543-3")).isEqualTo("9.876.543-3");
        assertThat(Formatos.formatearRut(" 12.345.678-5 ")).isEqualTo("12.345.678-5");
    }

    @Test
    void normalizaTelefonoParaWhatsapp() {
        assertThat(Formatos.telefonoWhatsapp("+56 9 0000 0001")).isEqualTo("56900000001");
        assertThat(Formatos.telefonoWhatsapp("9 0000 0001")).isEqualTo("56900000001");
        assertThat(Formatos.telefonoWhatsapp("(+56) 900000001")).isEqualTo("56900000001");
        assertThat(Formatos.telefonoWhatsapp("00000001")).isEqualTo("56900000001");
        assertThat(Formatos.telefonoWhatsapp("123")).isNull();
        assertThat(Formatos.telefonoWhatsapp(null)).isNull();
    }
}
