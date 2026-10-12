package com.taller.mecanico.entity;

import com.taller.mecanico.util.Precios;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Tarea estándar del taller con su precio sugerido. El precio se guarda tal
 * como se ingresó (neto o con IVA, según {@code precioIncluyeIva}) y el otro
 * valor se calcula.
 */
@Getter
@Setter
@Entity
@Table(name = "catalogo_trabajos")
public class CatalogoTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la tarea es obligatorio")
    @Column(name = "nombre_tarea", nullable = false)
    private String nombreTarea;

    // Precio tal como se ingresó
    @Column(name = "costo_estimado_defecto", precision = 10, scale = 2)
    private BigDecimal costoEstimadoDefecto;

    @Column(name = "precio_incluye_iva", nullable = false)
    private boolean precioIncluyeIva = false;

    @Column(nullable = false)
    private boolean activo = true;

    public BigDecimal getPrecioNeto() {
        return precioIncluyeIva ? Precios.neto(costoEstimadoDefecto) : costoEstimadoDefecto;
    }

    public BigDecimal getPrecioConIva() {
        return precioIncluyeIva ? costoEstimadoDefecto : Precios.conIva(costoEstimadoDefecto);
    }
}
