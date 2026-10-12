package com.taller.mecanico.entity;

import com.taller.mecanico.entity.comun.Auditoria;
import com.taller.mecanico.enums.MomentoFirma;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Firma del cliente en un momento de la orden (recepción o retiro).
 * Si {@code firmada} es false, se eligió "Continuar sin firma".
 * Quién la registró y cuándo queda en {@link Auditoria}.
 */
@Getter
@Setter
@Entity
@Table(name = "firmas_orden")
public class FirmaOrden extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenIngreso orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MomentoFirma momento;

    @Column(nullable = false)
    private boolean firmada;

    // Imagen PNG del trazo (null si no firmó)
    @Column(name = "imagen_png")
    private byte[] imagenPng;

    @Column(name = "nombre_firmante")
    private String nombreFirmante;

    @Column(name = "rut_firmante", length = 12)
    private String rutFirmante;

    @Column(nullable = false)
    private boolean anulada = false;

    @Column(name = "anulada_en")
    private LocalDateTime anuladaEn;

    /** La imagen lista para usar en un {@code <img src="...">} (pantalla y PDF). */
    public String getImagenDataUri() {
        return imagenPng == null ? null : "data:image/png;base64," + Base64.getEncoder().encodeToString(imagenPng);
    }
}
