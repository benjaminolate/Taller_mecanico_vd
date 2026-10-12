package com.taller.mecanico.enums;

/** Rol de un usuario del sistema. */
public enum Rol {
    JEFE("Jefe"),
    EMPLEADO("Empleado");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
