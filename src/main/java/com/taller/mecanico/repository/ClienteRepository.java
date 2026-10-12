package com.taller.mecanico.repository;

import com.taller.mecanico.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /** El RUT se guarda siempre con formato "12.345.678-5". */
    Optional<Cliente> findByRut(String rut);

    List<Cliente> findAllByOrderByNombreAsc();

    /**
     * Busca por parte del nombre, del teléfono o del RUT (este último sin
     * puntos ni guion, para que "12345678" encuentre "12.345.678-5").
     * Texto vacío = todos; rut vacío = no buscar por RUT. Se usan textos vacíos
     * en vez de null porque PostgreSQL no puede deducir el tipo de un null.
     */
    @Query("""
            SELECT c FROM Cliente c
            WHERE :texto = ''
               OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR c.telefono LIKE CONCAT('%', :texto, '%')
               OR (:rut <> '' AND UPPER(REPLACE(REPLACE(c.rut, '.', ''), '-', '')) LIKE CONCAT('%', :rut, '%'))
            """)
    Page<Cliente> buscar(@Param("texto") String texto, @Param("rut") String rut, Pageable pageable);
}
