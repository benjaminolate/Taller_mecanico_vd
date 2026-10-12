package com.taller.mecanico.entity.comun;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Quién creó y quién modificó por última vez un registro, y cuándo.
 * Spring completa estos campos solo, con el usuario conectado.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditoria {

    @CreatedBy
    @Column(name = "creado_por", length = 30, updatable = false)
    private String creadoPor;

    @LastModifiedBy
    @Column(name = "modificado_por", length = 30)
    private String modificadoPor;

    @CreatedDate
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @LastModifiedDate
    @Column(name = "modificado_en")
    private LocalDateTime modificadoEn;
}
