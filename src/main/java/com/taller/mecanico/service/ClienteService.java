package com.taller.mecanico.service;

import com.taller.mecanico.entity.Cliente;
import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.repository.ClienteRepository;
import com.taller.mecanico.repository.OrdenIngresoRepository;
import com.taller.mecanico.util.Formatos;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/** Búsqueda, edición e historial de clientes. */
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository      clienteRepo;
    private final OrdenIngresoRepository ordenRepo;

    @Transactional(readOnly = true)
    public Page<Cliente> buscar(String texto, Pageable pageable) {
        String limpio = StringUtils.hasText(texto) ? texto.trim() : null;
        String rut = limpio == null ? null : Formatos.limpiarRut(limpio);
        // Solo buscar por RUT si lo escrito tiene al menos 3 dígitos
        if (rut != null && rut.replaceAll("[^0-9]", "").length() < 3) {
            rut = null;
        }
        return clienteRepo.buscar(limpio == null ? "" : limpio, rut == null ? "" : rut, pageable);
    }

    @Transactional(readOnly = true)
    public Cliente buscar(Long id) {
        return clienteRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<OrdenIngreso> historial(Long clienteId) {
        return ordenRepo.findByClienteIdOrderByFechaDesc(clienteId);
    }

    @Transactional
    public Cliente actualizar(Long id, String nombre, String telefono, String rut, String relacionVehiculo) {
        Cliente c = buscar(id);
        if (!StringUtils.hasText(nombre)) {
            throw new NegocioException("El nombre del cliente es obligatorio.");
        }
        String rutFormateado = null;
        if (StringUtils.hasText(rut)) {
            if (!Formatos.rutValido(rut)) {
                throw new NegocioException("El RUT \"" + rut.trim()
                        + "\" no es válido. Revisa los números y el dígito verificador.");
            }
            rutFormateado = Formatos.formatearRut(rut);
            String rutFinal = rutFormateado;
            clienteRepo.findByRut(rutFinal)
                    .filter(otro -> !otro.getId().equals(id))
                    .ifPresent(otro -> {
                        throw new NegocioException("El RUT " + rutFinal + " ya pertenece al cliente " + otro.getNombre() + ".");
                    });
        }
        c.setNombre(nombre.trim());
        c.setTelefono(StringUtils.hasText(telefono) ? telefono.trim() : null);
        c.setRut(rutFormateado);
        c.setRelacionVehiculo(StringUtils.hasText(relacionVehiculo) ? relacionVehiculo.trim() : null);
        return clienteRepo.save(c);
    }
}
