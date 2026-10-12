package com.taller.mecanico.repository;

import com.taller.mecanico.entity.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    // Método derivado: Spring genera automáticamente la consulta SQL
    // a partir del nombre del método (busca por el campo "patente")
    Optional<Vehiculo> findByPatente(String patente);

    boolean existsByPatente(String patente);
}