package com.taller.mecanico.util;

import java.util.regex.Pattern;

/**
 * Normalización y validación de datos chilenos que se escriben a mano:
 * patentes y RUT. Así "demo-01" y "DEMO01" son la misma patente, y
 * "12345678-5" y "12.345.678-5" el mismo RUT.
 */
public final class Formatos {

    // 5 a 8 caracteres alfanuméricos con al menos una letra y un número
    // (cubre autos LLLLNN / LLNNNN, motos y patentes antiguas).
    private static final Pattern PATENTE = Pattern.compile("^(?=.*[A-Z])(?=.*[0-9])[A-Z0-9]{5,8}$");
    private static final Pattern RUT_LIMPIO = Pattern.compile("^[0-9]{7,9}[0-9K]$");

    private Formatos() {
    }

    // ── Patentes ───────────────────────────────────────────────────────

    /** Mayúsculas y sin espacios, guiones ni puntos. Devuelve null si viene vacía. */
    public static String normalizarPatente(String patente) {
        if (patente == null) return null;
        String limpia = patente.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return limpia.isEmpty() ? null : limpia;
    }

    /** Recibe una patente ya normalizada. */
    public static boolean patenteValida(String patenteNormalizada) {
        return patenteNormalizada != null && PATENTE.matcher(patenteNormalizada).matches();
    }

    // ── RUT ────────────────────────────────────────────────────────────

    /** Quita puntos, guion y espacios y pasa la K a mayúscula. Devuelve null si viene vacío. */
    public static String limpiarRut(String rut) {
        if (rut == null) return null;
        String limpio = rut.toUpperCase().replaceAll("[^0-9K]", "");
        return limpio.isEmpty() ? null : limpio;
    }

    /** Verifica el dígito verificador (módulo 11). Acepta el RUT con o sin puntos y guion. */
    public static boolean rutValido(String rut) {
        String limpio = limpiarRut(rut);
        if (limpio == null || !RUT_LIMPIO.matcher(limpio).matches()) return false;
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dv = limpio.charAt(limpio.length() - 1);
        return calcularDv(cuerpo) == dv;
    }

    /**
     * Devuelve el RUT con formato "12.345.678-5", o null si viene vacío.
     * No valida: usar {@link #rutValido(String)} antes.
     */
    public static String formatearRut(String rut) {
        String limpio = limpiarRut(rut);
        if (limpio == null || limpio.length() < 2) return limpio;
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dv = limpio.charAt(limpio.length() - 1);
        StringBuilder conPuntos = new StringBuilder();
        int cuenta = 0;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            conPuntos.insert(0, cuerpo.charAt(i));
            if (++cuenta % 3 == 0 && i > 0) conPuntos.insert(0, '.');
        }
        return conPuntos + "-" + dv;
    }

    static char calcularDv(String cuerpo) {
        int suma = 0;
        int factor = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cuerpo.charAt(i)) * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) return '0';
        if (resto == 10) return 'K';
        return (char) ('0' + resto);
    }

    /**
     * Número para WhatsApp: solo dígitos, con el código de Chile (56).
     * "9 0000 0001" y "+56 9 0000 0001" dan "56900000001".
     * Devuelve null si no parece un teléfono.
     */
    public static String telefonoWhatsapp(String telefono) {
        if (telefono == null) return null;
        String digitos = telefono.replaceAll("\\D", "");
        if (digitos.length() == 9 && digitos.startsWith("9")) {
            return "56" + digitos;            // celular chileno sin código de país
        }
        if (digitos.length() == 8) {
            return "569" + digitos;           // formato antiguo de 8 dígitos
        }
        return digitos.length() >= 10 ? digitos : null;
    }
}
