package com.taller.mecanico.repository;

import com.taller.mecanico.entity.Mecanico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MecanicoRepository extends JpaRepository<Mecanico, Long> {
    // Lista solo los mecánicos activos, ordenados alfabéticamente,
    // para poblar el <select> del formulario de trabajos.
    List<Mecanico> findByEstadoActivoTrueOrderByNombreAsc();

    // Todos los mecánicos (también los inactivos), para filtrar el historial.
    List<Mecanico> findAllByOrderByNombreAsc();

    List<Mecanico> findAllByOrderByEstadoActivoDescNombreAsc();

    Optional<Mecanico> findByRut(String rut);
}
