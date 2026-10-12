package com.taller.mecanico.entity;

import com.taller.mecanico.enums.EstadoOrden;
import com.taller.mecanico.enums.TipoIngreso;
import com.taller.mecanico.entity.comun.Auditoria;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "ordenes_ingreso")
public class OrdenIngreso extends Auditoria {

    // ID con formato "ORD-001", "ORD-002", etc. (lo generamos en el servicio)
    @Id
    @Column(name = "id", length = 20)
    private String id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    // Indica si es ingreso de vehículo completo o solo cremallera
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ingreso", nullable = false)
    private TipoIngreso tipoIngreso;

    // Estado actual de la orden
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrden estado;

    // Muchas órdenes pueden pertenecer a un cliente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // El vehículo es opcional (puede ser null si es solo cremallera)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = true)
    private Vehiculo vehiculo;

    // Relación 1 a 1 con el estado técnico del vehículo
    @OneToOne(mappedBy = "orden", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private EstadoVehiculo estadoVehiculo;

    // Relación 1 a 1 con el estado técnico de la cremallera
    @OneToOne(mappedBy = "orden", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private EstadoCremallera estadoCremallera;

    // Mecánico que recibió el vehículo (obligatorio al crear la orden)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recibido_por_id")
    private Mecanico recibidoPor;

    // Mecánico que realiza el trabajo. Se asigna después de crear la orden
    // (puede quedar "sin asignar" uno o dos días).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mecanico_id")
    private Mecanico mecanico;

    // Trabajos técnicos de la orden. Si se borra la orden, se borran sus trabajos.
    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    private List<TrabajoRealizado> trabajos = new ArrayList<>();


    /** Días desde que ingresó la orden (para el panel de inicio). */
    public long getDiasDesdeIngreso() {
        return fecha == null ? 0 : java.time.temporal.ChronoUnit.DAYS.between(fecha.toLocalDate(), java.time.LocalDate.now());
    }
}
