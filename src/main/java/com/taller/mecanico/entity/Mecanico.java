package com.taller.mecanico.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "mecanicos")
public class Mecanico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del mecánico es obligatorio")
    @Column(nullable = false)
    private String nombre;

    // Formato chileno con puntos y guion (ej: "12.345.678-9")
    @Column(length = 12, unique = true)
    private String rut;

    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo = true;
}
