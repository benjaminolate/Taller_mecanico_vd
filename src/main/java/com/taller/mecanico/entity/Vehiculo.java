package com.taller.mecanico.entity;

import com.taller.mecanico.enums.TipoVehiculo;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vehiculos")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // La patente debe ser única en toda la base de datos
    @NotBlank(message = "La patente es obligatoria")
    @Column(nullable = false, unique = true, length = 10)
    private String patente;

    @NotBlank(message = "La marca es obligatoria")
    @Column(nullable = false)
    private String marca;

    @NotBlank(message = "El modelo es obligatorio")
    @Column(nullable = false)
    private String modelo;

    @Column(nullable = true)
    private Integer anio;

    // Guardamos el enum como texto legible en la BD (ej: "AUTOMOVIL")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo")
    private TipoVehiculo tipoVehiculo;
}