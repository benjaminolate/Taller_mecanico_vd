package com.taller.mecanico.repository;

import com.taller.mecanico.entity.CatalogoTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogoTrabajoRepository extends JpaRepository<CatalogoTrabajo, Long> {

    List<CatalogoTrabajo> findAllByOrderByActivoDescNombreTareaAsc();

    // Solo las tareas vigentes, para el formulario de trabajos
    List<CatalogoTrabajo> findByActivoTrueOrderByNombreTareaAsc();
}
