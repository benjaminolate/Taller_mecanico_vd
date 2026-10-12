package com.taller.mecanico.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "estados_cremallera")
public class EstadoCremallera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dientes_daniados")
    private Boolean dienteDaniados = false;

    @Column(name = "fugas_aceite")
    private Boolean fugasAceite = false;

    @Column(name = "juego_excesivo")
    private Boolean juegoExcesivo = false;

    @Column(name = "bujes_gastados")
    private Boolean bujesGastados = false;

    @Column(name = "marca_cremallera")
    private String marcaCremallera;

    @Column(name = "es_original")
    private Boolean esOriginal = true;

    @Column(name = "obs_cremallera", columnDefinition = "TEXT")
    private String obsCremallera;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenIngreso orden;
}