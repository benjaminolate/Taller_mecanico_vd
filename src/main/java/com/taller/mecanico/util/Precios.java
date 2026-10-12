package com.taller.mecanico.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Cálculos de IVA (19 % en Chile). Los montos se redondean a pesos enteros. */
public final class Precios {

    public static final BigDecimal IVA = new BigDecimal("0.19");
    private static final BigDecimal FACTOR = BigDecimal.ONE.add(IVA);

    private Precios() {
    }

    public static BigDecimal conIva(BigDecimal neto) {
        if (neto == null) return null;
        return neto.multiply(FACTOR).setScale(0, RoundingMode.HALF_UP);
    }

    public static BigDecimal neto(BigDecimal conIva) {
        if (conIva == null) return null;
        return conIva.divide(FACTOR, 0, RoundingMode.HALF_UP);
    }
}
