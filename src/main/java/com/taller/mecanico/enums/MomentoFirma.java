package com.taller.mecanico.enums;

/** Momento en que firma el cliente. */
public enum MomentoFirma {
    RECEPCION("Recepción", "Entrega del vehículo al taller"),
    RETIRO("Retiro", "Retiro del vehículo conforme");

    private final String etiqueta;
    private final String descripcion;

    MomentoFirma(String etiqueta, String descripcion) {
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Para las direcciones web: "recepcion" / "retiro". */
    public String getRuta() {
        return name().toLowerCase();
    }
}
