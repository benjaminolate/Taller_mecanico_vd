package com.taller.mecanico.service;

import com.taller.mecanico.entity.CatalogoTrabajo;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.repository.CatalogoTrabajoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/** Tareas estándar con precio sugerido (neto o con IVA). */
@Service
@RequiredArgsConstructor
public class CatalogoTrabajoService {

    private final CatalogoTrabajoRepository catalogoRepo;

    @Transactional(readOnly = true)
    public List<CatalogoTrabajo> listar() {
        return catalogoRepo.findAllByOrderByActivoDescNombreTareaAsc();
    }

    @Transactional(readOnly = true)
    public List<CatalogoTrabajo> listarActivos() {
        return catalogoRepo.findByActivoTrueOrderByNombreTareaAsc();
    }

    @Transactional(readOnly = true)
    public CatalogoTrabajo buscar(Long id) {
        return catalogoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tarea del catálogo no encontrada: " + id));
    }

    /** Crea (id null) o actualiza una tarea del catálogo. */
    @Transactional
    public CatalogoTrabajo guardar(Long id, String nombre, BigDecimal precio, boolean incluyeIva, boolean activo) {
        if (!StringUtils.hasText(nombre)) {
            throw new NegocioException("El nombre de la tarea es obligatorio.");
        }
        if (precio != null && precio.signum() < 0) {
            throw new NegocioException("El precio no puede ser negativo.");
        }
        CatalogoTrabajo c = id == null ? new CatalogoTrabajo() : buscar(id);
        c.setNombreTarea(nombre.trim());
        c.setCostoEstimadoDefecto(precio);
        c.setPrecioIncluyeIva(incluyeIva);
        c.setActivo(activo);
        return catalogoRepo.save(c);
    }
}
