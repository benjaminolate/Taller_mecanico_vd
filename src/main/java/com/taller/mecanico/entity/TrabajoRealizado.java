package com.taller.mecanico.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.taller.mecanico.enums.CategoriaTrabajo;
import com.taller.mecanico.entity.comun.Auditoria;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa un trabajo técnico registrado dentro de una OrdenIngreso.
 */
@Getter
@Setter
@Entity
@Table(name = "trabajos_realizados")
public class TrabajoRealizado extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenIngreso orden;

    @NotBlank(message = "La descripción del trabajo es obligatoria")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @NotNull(message = "Debe seleccionar una categoría")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaTrabajo categoria;

    // Mecánico responsable (opcional).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mecanico_id", nullable = true)
    private Mecanico mecanico;

    @Column(name = "costo_estimado", precision = 10, scale = 2)
    private BigDecimal costoEstimado;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(nullable = false)
    private Boolean completado = false;

    @CreationTimestamp
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_completado")
    private LocalDateTime fechaCompletado;
}
