package com.taller.mecanico.entity;

import com.taller.mecanico.enums.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Persona que entra al sistema con usuario y contraseña. */
@Getter
@Setter
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre de usuario para entrar, en minúsculas (ej: "pedro")
    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;

    // Contraseña cifrada con BCrypt; nunca se guarda en texto plano
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    // true cuando el jefe asignó una contraseña temporal
    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword = false;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;
}
