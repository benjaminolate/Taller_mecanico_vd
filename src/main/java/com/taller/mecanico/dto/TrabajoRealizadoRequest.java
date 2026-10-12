package com.taller.mecanico.dto;

import com.taller.mecanico.enums.CategoriaTrabajo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO que recibe el formulario para registrar un TrabajoRealizado.
 * "mecanicoId" referencia al catálogo de mecánicos; "catalogoTrabajoId" solo
 * se usa en el navegador para autocompletar descripción y costo.
 */
@Data
public class TrabajoRealizadoRequest {

    private String ordenId;

    @NotBlank(message = "La descripción del trabajo es obligatoria")
    private String descripcion;

    @NotNull(message = "Debe seleccionar una categoría")
    private CategoriaTrabajo categoria;

    private Long mecanicoId;

    private Long catalogoTrabajoId;

    @DecimalMin(value = "0.0", inclusive = true, message = "El costo no puede ser negativo")
    private BigDecimal costoEstimado;

    private String observaciones;

    private Boolean completado = false;
}
