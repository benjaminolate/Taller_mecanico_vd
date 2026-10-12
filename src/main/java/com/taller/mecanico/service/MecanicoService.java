package com.taller.mecanico.service;

import com.taller.mecanico.entity.Mecanico;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.repository.MecanicoRepository;
import com.taller.mecanico.util.Formatos;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Catálogo de mecánicos. No se borran (sus trabajos quedan en el historial):
 * se desactivan para que no aparezcan al registrar trabajos nuevos.
 */
@Service
@RequiredArgsConstructor
public class MecanicoService {

    private final MecanicoRepository mecanicoRepo;

    @Transactional(readOnly = true)
    public List<Mecanico> listar() {
        return mecanicoRepo.findAllByOrderByEstadoActivoDescNombreAsc();
    }

    @Transactional(readOnly = true)
    public Mecanico buscar(Long id) {
        return mecanicoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Mecánico no encontrado: " + id));
    }

    /** Crea (id null) o actualiza un mecánico. */
    @Transactional
    public Mecanico guardar(Long id, String nombre, String rut, boolean activo) {
        if (!StringUtils.hasText(nombre)) {
            throw new NegocioException("El nombre del mecánico es obligatorio.");
        }
        String rutFormateado = null;
        if (StringUtils.hasText(rut)) {
            if (!Formatos.rutValido(rut)) {
                throw new NegocioException("El RUT \"" + rut.trim() + "\" no es válido.");
            }
            rutFormateado = Formatos.formatearRut(rut);
            String rutFinal = rutFormateado;
            mecanicoRepo.findByRut(rutFinal)
                    .filter(otro -> !otro.getId().equals(id))
                    .ifPresent(otro -> {
                        throw new NegocioException("El RUT " + rutFinal + " ya está asignado a " + otro.getNombre() + ".");
                    });
        }
        Mecanico m = id == null ? new Mecanico() : buscar(id);
        m.setNombre(nombre.trim());
        m.setRut(rutFormateado);
        m.setEstadoActivo(activo);
        return mecanicoRepo.save(m);
    }
}
