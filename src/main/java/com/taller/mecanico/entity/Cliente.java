package com.taller.mecanico.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false)
    private String nombre;

    @Column(nullable = true)
    private String telefono;

    /**
     * RUT chileno con puntos y guion (ej: "12.345.678-9"). Único pero opcional:
     * PostgreSQL permite varios NULL en una columna UNIQUE.
     */
    @Column(length = 12, unique = true)
    private String rut;

    // Descripción de la relación del cliente con el vehículo
    // Ej: "Dueño", "Arrendatario", "Empresa"
    @Column(name = "relacion_vehiculo")
    private String relacionVehiculo;

    // Un cliente puede tener múltiples órdenes de ingreso
    // @JsonIgnore evita el bucle infinito al serializar a JSON:
    // Cliente -> ordenes -> OrdenIngreso -> cliente -> ordenes -> ... (ciclo infinito)
    @JsonIgnore
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrdenIngreso> ordenes;

    /** Teléfono listo para los enlaces de WhatsApp (ver Formatos.telefonoWhatsapp). */
    @Transient
    @JsonIgnore
    public String getTelefonoWhatsapp() {
        return com.taller.mecanico.util.Formatos.telefonoWhatsapp(telefono);
    }
}
