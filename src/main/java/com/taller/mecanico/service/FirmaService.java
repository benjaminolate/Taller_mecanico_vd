package com.taller.mecanico.service;

import com.taller.mecanico.entity.FirmaOrden;
import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.enums.EstadoOrden;
import com.taller.mecanico.enums.MomentoFirma;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.repository.FirmaOrdenRepository;
import com.taller.mecanico.repository.OrdenIngresoRepository;
import com.taller.mecanico.util.Formatos;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Firmas del cliente:
 * <ul>
 *   <li>Recepción: mientras la orden no esté entregada.</li>
 *   <li>Retiro: cuando la orden está lista o ya entregada.</li>
 *   <li>Firmar es opcional: "Continuar sin firma" deja registro de que no firmó.</li>
 *   <li>Una firma no se edita; si hubo un error se anula y se vuelve a registrar.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FirmaService {

    private static final String PREFIJO_PNG = "data:image/png;base64,";
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final int TAMANO_MAXIMO = 1_000_000; // 1 MB

    private final FirmaOrdenRepository   firmaRepo;
    private final OrdenIngresoRepository ordenRepo;

    @Transactional(readOnly = true)
    public Map<MomentoFirma, FirmaOrden> vigentes(String ordenId) {
        Map<MomentoFirma, FirmaOrden> mapa = new EnumMap<>(MomentoFirma.class);
        firmaRepo.findByOrdenIdAndAnuladaFalse(ordenId).forEach(f -> mapa.put(f.getMomento(), f));
        return mapa;
    }

    @Transactional(readOnly = true)
    public List<OrdenIngreso> pendientes(MomentoFirma momento) {
        return momento == MomentoFirma.RECEPCION
                ? firmaRepo.pendientesDeRecepcion(momento)
                : firmaRepo.pendientesDeRetiro(momento);
    }

    /** Lanza NegocioException si en el estado actual de la orden no corresponde esta firma. */
    @Transactional(readOnly = true)
    public void verificarQueSePuedeFirmar(OrdenIngreso orden, MomentoFirma momento) {
        if (momento == MomentoFirma.RECEPCION && orden.getEstado() == EstadoOrden.ENTREGADO) {
            throw new NegocioException("La orden " + orden.getId() + " ya fue entregada: no corresponde firma de recepción.");
        }
        if (momento == MomentoFirma.RETIRO
                && orden.getEstado() != EstadoOrden.LISTO && orden.getEstado() != EstadoOrden.ENTREGADO) {
            throw new NegocioException("La firma de retiro se registra cuando la orden está Lista. "
                    + "Estado actual: " + orden.getEstado().getEtiqueta() + ".");
        }
        firmaRepo.findByOrdenIdAndMomentoAndAnuladaFalse(orden.getId(), momento).ifPresent(f -> {
            throw new NegocioException("La orden " + orden.getId() + " ya tiene registrada la "
                    + momento.getEtiqueta().toLowerCase() + ". Si hubo un error, anúlala y vuelve a registrarla.");
        });
    }

    /**
     * Registra la firma. Con {@code imagenDataUri} null se registra "sin firma".
     * El nombre es obligatorio solo si firma; el RUT es opcional y se valida.
     */
    @Transactional
    public FirmaOrden registrar(String ordenId, MomentoFirma momento, String imagenDataUri,
                                String nombre, String rut) {
        OrdenIngreso orden = ordenRepo.findById(ordenId)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + ordenId));
        verificarQueSePuedeFirmar(orden, momento);

        FirmaOrden f = new FirmaOrden();
        f.setOrden(orden);
        f.setMomento(momento);

        boolean firma = StringUtils.hasText(imagenDataUri);
        f.setFirmada(firma);
        if (firma) {
            if (!StringUtils.hasText(nombre)) {
                throw new NegocioException("Escribe el nombre de quien firma.");
            }
            f.setImagenPng(decodificarPng(imagenDataUri));
        }
        if (StringUtils.hasText(nombre)) {
            f.setNombreFirmante(nombre.trim());
        }
        if (StringUtils.hasText(rut)) {
            if (!Formatos.rutValido(rut)) {
                throw new NegocioException("El RUT \"" + rut.trim() + "\" no es válido.");
            }
            f.setRutFirmante(Formatos.formatearRut(rut));
        }
        FirmaOrden guardada = firmaRepo.save(f);
        log.info("Orden {}: {} {}", ordenId, momento, firma ? "firmada" : "sin firma");
        return guardada;
    }

    /** Anula una firma vigente para poder registrarla de nuevo. */
    @Transactional
    public FirmaOrden anular(Long firmaId) {
        FirmaOrden f = firmaRepo.findById(firmaId)
                .orElseThrow(() -> new EntityNotFoundException("Firma no encontrada: " + firmaId));
        if (f.isAnulada()) {
            throw new NegocioException("Esta firma ya estaba anulada.");
        }
        if (f.getOrden().getEstado() == EstadoOrden.ENTREGADO && f.getMomento() == MomentoFirma.RECEPCION) {
            throw new NegocioException("La orden ya fue entregada: la firma de recepción no se puede anular.");
        }
        f.setAnulada(true);
        f.setAnuladaEn(LocalDateTime.now());
        log.warn("Orden {}: se anuló la firma de {}", f.getOrden().getId(), f.getMomento());
        return firmaRepo.save(f);
    }

    private byte[] decodificarPng(String dataUri) {
        if (!dataUri.startsWith(PREFIJO_PNG)) {
            throw new NegocioException("La firma no llegó en el formato esperado. Intenta firmar de nuevo.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dataUri.substring(PREFIJO_PNG.length()));
        } catch (IllegalArgumentException e) {
            throw new NegocioException("La firma no llegó en el formato esperado. Intenta firmar de nuevo.");
        }
        if (bytes.length < FIRMA_PNG.length || bytes.length > TAMANO_MAXIMO) {
            throw new NegocioException("La imagen de la firma no es válida. Intenta firmar de nuevo.");
        }
        for (int i = 0; i < FIRMA_PNG.length; i++) {
            if (bytes[i] != FIRMA_PNG[i]) {
                throw new NegocioException("La imagen de la firma no es válida. Intenta firmar de nuevo.");
            }
        }
        return bytes;
    }
}
