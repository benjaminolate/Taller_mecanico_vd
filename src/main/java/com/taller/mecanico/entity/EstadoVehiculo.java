package com.taller.mecanico.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "estados_vehiculo")
public class EstadoVehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rayones")
    private Boolean rayones = false;

    @Column(name = "golpes")
    private Boolean golpes = false;

    @Column(name = "obs_carroceria", columnDefinition = "TEXT")
    private String obsCarroceria;

    @Column(name = "nivel_combustible")
    private Integer nivelCombustible;

    @Column(name = "nivel_aceite")
    private Boolean nivelAceiteBajo = false;

    @Column(name = "nivel_agua")
    private Boolean nivelAguaBajo = false;

    @Column(name = "neumaticos_ok")
    private Boolean neumaticosOk = true;

    @Column(name = "obs_neumaticos", columnDefinition = "TEXT")
    private String obsNeumaticos;

    @Column(name = "obs_generales", columnDefinition = "TEXT")
    private String obsGenerales;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenIngreso orden;
}