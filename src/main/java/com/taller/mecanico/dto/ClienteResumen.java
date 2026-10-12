package com.taller.mecanico.dto;

import com.taller.mecanico.entity.Cliente;

/** Datos de un cliente para el autocompletado del formulario de nueva orden. */
public record ClienteResumen(Long id, String nombre, String telefono, String rut, String relacionVehiculo) {

    public static ClienteResumen de(Cliente c) {
        return new ClienteResumen(c.getId(), c.getNombre(), c.getTelefono(), c.getRut(), c.getRelacionVehiculo());
    }
}
