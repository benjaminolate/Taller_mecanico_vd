package com.taller.mecanico.dto;

import com.taller.mecanico.entity.Vehiculo;
import com.taller.mecanico.enums.TipoVehiculo;

/** Datos de un vehículo para el autocompletado por patente. */
public record VehiculoResumen(Long id, String patente, String marca, String modelo, Integer anio,
                              TipoVehiculo tipoVehiculo) {

    public static VehiculoResumen de(Vehiculo v) {
        return new VehiculoResumen(v.getId(), v.getPatente(), v.getMarca(), v.getModelo(), v.getAnio(),
                v.getTipoVehiculo());
    }
}
