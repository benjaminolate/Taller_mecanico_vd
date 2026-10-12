package com.taller.mecanico.service;

import com.taller.mecanico.enums.VistaHistorial;
import com.taller.mecanico.dto.OrdenBusquedaDTO;
import com.taller.mecanico.dto.OrdenIngresoRequest;
import com.taller.mecanico.entity.*;
import com.taller.mecanico.enums.EstadoOrden;
import com.taller.mecanico.enums.TipoIngreso;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.repository.ClienteRepository;
import com.taller.mecanico.repository.MecanicoRepository;
import com.taller.mecanico.repository.OrdenIngresoRepository;
import com.taller.mecanico.repository.VehiculoRepository;
import com.taller.mecanico.specification.OrdenIngresoSpecification;
import com.taller.mecanico.util.Formatos;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Creación y búsqueda de órdenes de ingreso. La búsqueda y la paginación se
 * resuelven en la base de datos (OrdenIngresoSpecification); los checkbox que
 * el formulario no envía llegan como null y se tratan como false.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrdenIngresoService {

    private final ClienteRepository      clienteRepository;
    private final VehiculoRepository     vehiculoRepository;
    private final OrdenIngresoRepository ordenIngresoRepository;
    private final MecanicoRepository     mecanicoRepository;

    // ── Crear Orden completa ─────────────────────────────────────────────────
    @Transactional
    public OrdenIngreso crearOrdenCompleta(OrdenIngresoRequest request) {
        normalizarYValidar(request);

        // 1. Resolver cliente (reutilizar o crear)
        Cliente cliente = resolverCliente(request);

        // 2. Resolver vehículo (solo si el ingreso es de vehículo)
        Vehiculo vehiculo = null;
        if (request.getTipoIngreso() == TipoIngreso.VEHICULO) {
            vehiculo = resolverVehiculo(request);
        }

        // 3. Crear la OrdenIngreso base
        OrdenIngreso orden = new OrdenIngreso();
        orden.setId(generarCorrelativo());
        orden.setFecha(LocalDateTime.now());
        orden.setTipoIngreso(request.getTipoIngreso());
        orden.setEstado(EstadoOrden.PENDIENTE);
        orden.setCliente(cliente);
        orden.setVehiculo(vehiculo);
        orden.setRecibidoPor(resolverMecanico(request.getRecibidoPorId(), "Elige quién recibe el vehículo."));
        // El mecánico que realiza el trabajo se asigna después (asignarMecanico)

        // Guardamos la orden para que su ID exista antes de asociar los estados
        OrdenIngreso ordenGuardada = ordenIngresoRepository.save(orden);

        // 4. Estado del Vehículo (solo si tipoIngreso == VEHICULO)
        if (request.getTipoIngreso() == TipoIngreso.VEHICULO) {
            EstadoVehiculo ev = new EstadoVehiculo();
            ev.setRayones(        Boolean.TRUE.equals(request.getRayones()));
            ev.setGolpes(         Boolean.TRUE.equals(request.getGolpes()));
            ev.setObsCarroceria(  request.getObsCarroceria());
            ev.setNivelCombustible(request.getNivelCombustible());
            ev.setNivelAceiteBajo(Boolean.TRUE.equals(request.getNivelAceiteBajo()));
            ev.setNivelAguaBajo(  Boolean.TRUE.equals(request.getNivelAguaBajo()));
            ev.setNeumaticosOk(   !Boolean.FALSE.equals(request.getNeumaticosOk())); // default true
            ev.setObsNeumaticos(  request.getObsNeumaticos());
            ev.setObsGenerales(   request.getObsGenerales());
            ev.setOrden(ordenGuardada);
            ordenGuardada.setEstadoVehiculo(ev);
        }

        // 5. Estado de la Cremallera (solo si tipoIngreso == CREMALLERA)
        if (request.getTipoIngreso() == TipoIngreso.CREMALLERA) {
            EstadoCremallera ec = new EstadoCremallera();
            ec.setDienteDaniados(Boolean.TRUE.equals(request.getDienteDaniados()));
            ec.setFugasAceite(   Boolean.TRUE.equals(request.getFugasAceite()));
            ec.setJuegoExcesivo( Boolean.TRUE.equals(request.getJuegoExcesivo()));
            ec.setBujesGastados( Boolean.TRUE.equals(request.getBujesGastados()));
            ec.setMarcaCremallera(request.getMarcaCremallera());
            ec.setEsOriginal(    !Boolean.FALSE.equals(request.getEsOriginal())); // default true
            ec.setObsCremallera( request.getObsCremallera());
            ec.setOrden(ordenGuardada);
            ordenGuardada.setEstadoCremallera(ec);
        }

        // El cascade=ALL en la entidad guarda los estados junto con la orden
        OrdenIngreso resultado = ordenIngresoRepository.save(ordenGuardada);
        log.info("Orden {} creada para cliente '{}' (tipoIngreso={})",
                resultado.getId(), cliente.getNombre(), request.getTipoIngreso());
        return resultado;
    }

    // ── Buscar órdenes con filtros dinámicos + paginación ────────────────────
    @Transactional(readOnly = true)
    public Page<OrdenIngreso> buscarOrdenes(OrdenBusquedaDTO criterios, Pageable pageable) {
        Specification<OrdenIngreso> spec = OrdenIngresoSpecification.conFiltros(criterios);
        return ordenIngresoRepository.findAll(spec, pageable);
    }

    /** Cuántas órdenes hay en cada pestaña del historial, respetando los demás filtros. */
    @Transactional(readOnly = true)
    public java.util.Map<VistaHistorial, Long> contarPorVista(OrdenBusquedaDTO criterios) {
        java.util.Map<VistaHistorial, Long> conteo = new java.util.EnumMap<>(VistaHistorial.class);
        for (VistaHistorial v : VistaHistorial.values()) {
            conteo.put(v, ordenIngresoRepository.count(OrdenIngresoSpecification.conFiltros(criterios.enVista(v))));
        }
        return conteo;
    }

    // ── Editar una orden existente ───────────────────────────────────────────

    /** Arma el formulario de edición con los datos actuales de la orden. */
    @Transactional(readOnly = true)
    public OrdenIngresoRequest formularioEdicion(String id) {
        OrdenIngreso orden = buscar(id);
        OrdenIngresoRequest r = new OrdenIngresoRequest();
        r.setClienteId(orden.getCliente().getId());
        r.setClienteNombre(orden.getCliente().getNombre());
        r.setTipoIngreso(orden.getTipoIngreso());
        r.setRecibidoPorId(orden.getRecibidoPor() != null ? orden.getRecibidoPor().getId() : null);
        r.setMecanicoId(orden.getMecanico() != null ? orden.getMecanico().getId() : null);
        Vehiculo v = orden.getVehiculo();
        if (v != null) {
            r.setVehiculoPatente(v.getPatente());
            r.setVehiculoMarca(v.getMarca());
            r.setVehiculoModelo(v.getModelo());
            r.setVehiculoAnio(v.getAnio());
            r.setVehiculoTipo(v.getTipoVehiculo());
        }
        EstadoVehiculo ev = orden.getEstadoVehiculo();
        if (ev != null) {
            r.setRayones(ev.getRayones());
            r.setGolpes(ev.getGolpes());
            r.setObsCarroceria(ev.getObsCarroceria());
            r.setNivelCombustible(ev.getNivelCombustible());
            r.setNivelAceiteBajo(ev.getNivelAceiteBajo());
            r.setNivelAguaBajo(ev.getNivelAguaBajo());
            r.setNeumaticosOk(ev.getNeumaticosOk());
            r.setObsNeumaticos(ev.getObsNeumaticos());
            r.setObsGenerales(ev.getObsGenerales());
        }
        EstadoCremallera ec = orden.getEstadoCremallera();
        if (ec != null) {
            r.setDienteDaniados(ec.getDienteDaniados());
            r.setFugasAceite(ec.getFugasAceite());
            r.setJuegoExcesivo(ec.getJuegoExcesivo());
            r.setBujesGastados(ec.getBujesGastados());
            r.setMarcaCremallera(ec.getMarcaCremallera());
            r.setEsOriginal(ec.getEsOriginal());
            r.setObsCremallera(ec.getObsCremallera());
        }
        return r;
    }

    /**
     * Corrige una orden: cliente (uno ya registrado), vehículo y checklist de
     * ingreso. El tipo de ingreso no cambia. Una orden entregada no se edita.
     */
    @Transactional
    public OrdenIngreso editarOrden(String id, OrdenIngresoRequest request) {
        OrdenIngreso orden = buscar(id);
        if (orden.getEstado().esFinal()) {
            throw new NegocioException("La orden " + id + " ya fue entregada y no se puede editar.");
        }
        if (request.getClienteId() == null) {
            throw new NegocioException("Debes elegir el cliente de la orden.");
        }
        request.setTipoIngreso(orden.getTipoIngreso());
        request.setClienteRut(null);
        normalizarYValidar(request);

        orden.setRecibidoPor(resolverMecanico(request.getRecibidoPorId(), "Elige quién recibe el vehículo."));
        orden.setMecanico(request.getMecanicoId() == null ? null
                : resolverMecanico(request.getMecanicoId(), "Elige el mecánico que realiza el trabajo."));
        orden.setCliente(clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + request.getClienteId())));

        if (orden.getTipoIngreso() == TipoIngreso.VEHICULO) {
            orden.setVehiculo(resolverVehiculo(request));
            EstadoVehiculo ev = orden.getEstadoVehiculo();
            if (ev == null) {
                ev = new EstadoVehiculo();
                ev.setOrden(orden);
                orden.setEstadoVehiculo(ev);
            }
            ev.setRayones(        Boolean.TRUE.equals(request.getRayones()));
            ev.setGolpes(         Boolean.TRUE.equals(request.getGolpes()));
            ev.setNivelCombustible(request.getNivelCombustible());
            ev.setNivelAceiteBajo(Boolean.TRUE.equals(request.getNivelAceiteBajo()));
            ev.setNivelAguaBajo(  Boolean.TRUE.equals(request.getNivelAguaBajo()));
            ev.setObsGenerales(   request.getObsGenerales());
        } else {
            EstadoCremallera ec = orden.getEstadoCremallera();
            if (ec == null) {
                ec = new EstadoCremallera();
                ec.setOrden(orden);
                orden.setEstadoCremallera(ec);
            }
            ec.setDienteDaniados(Boolean.TRUE.equals(request.getDienteDaniados()));
            ec.setFugasAceite(   Boolean.TRUE.equals(request.getFugasAceite()));
            ec.setJuegoExcesivo( Boolean.TRUE.equals(request.getJuegoExcesivo()));
            ec.setBujesGastados( Boolean.TRUE.equals(request.getBujesGastados()));
            ec.setMarcaCremallera(request.getMarcaCremallera());
            ec.setEsOriginal(    !Boolean.FALSE.equals(request.getEsOriginal()));
            ec.setObsCremallera( request.getObsCremallera());
        }
        OrdenIngreso guardada = ordenIngresoRepository.save(orden);
        log.info("Orden {} editada", id);
        return guardada;
    }

    @Transactional(readOnly = true)
    public OrdenIngreso buscar(String id) {
        return ordenIngresoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + id));
    }

    // ── Validación ───────────────────────────────────────────────────────────

    /**
     * Deja patente y RUT en su formato estándar y revisa los datos obligatorios.
     * Lanza NegocioException con un mensaje para el usuario si algo no calza.
     */
    void normalizarYValidar(OrdenIngresoRequest request) {
        if (StringUtils.hasText(request.getClienteRut())) {
            if (!Formatos.rutValido(request.getClienteRut())) {
                throw new NegocioException("El RUT \"" + request.getClienteRut().trim()
                        + "\" no es válido. Revisa los números y el dígito verificador.");
            }
            request.setClienteRut(Formatos.formatearRut(request.getClienteRut()));
        } else {
            request.setClienteRut(null);
        }

        if (request.getTipoIngreso() == TipoIngreso.VEHICULO) {
            String patente = Formatos.normalizarPatente(request.getVehiculoPatente());
            if (patente == null) {
                throw new NegocioException("Para un ingreso de vehículo la patente es obligatoria.");
            }
            if (!Formatos.patenteValida(patente)) {
                throw new NegocioException("La patente \"" + request.getVehiculoPatente().trim()
                        + "\" no es válida. Usa solo letras y números, por ejemplo DEMO01.");
            }
            request.setVehiculoPatente(patente);
            boolean vehiculoExiste = vehiculoRepository.existsByPatente(patente);
            if (!vehiculoExiste && (!StringUtils.hasText(request.getVehiculoMarca())
                    || !StringUtils.hasText(request.getVehiculoModelo()))) {
                throw new NegocioException("El vehículo " + patente
                        + " es nuevo: completa la marca y el modelo.");
            }
        }
    }

    // ── Asignar el mecánico que realiza el trabajo ──────────────────────────

    /**
     * Asigna (o cambia) el mecánico que realiza el trabajo. Se hace después de
     * crear la orden, desde el historial o el detalle. {@code null} = sin asignar.
     */
    @Transactional
    public OrdenIngreso asignarMecanico(String ordenId, Long mecanicoId) {
        OrdenIngreso orden = buscar(ordenId);
        if (mecanicoId == null) {
            orden.setMecanico(null);
        } else {
            Mecanico m = resolverMecanico(mecanicoId, "Elige el mecánico que realiza el trabajo.");
            if (!Boolean.TRUE.equals(m.getEstadoActivo())
                    && (orden.getMecanico() == null || !orden.getMecanico().getId().equals(m.getId()))) {
                throw new NegocioException("El mecánico " + m.getNombre() + " está inactivo.");
            }
            orden.setMecanico(m);
        }
        return ordenIngresoRepository.save(orden);
    }

    // ── Privados auxiliares ──────────────────────────────────────────────────

    /** Busca un mecánico obligatorio; si falta, avisa con el mensaje indicado. */
    private Mecanico resolverMecanico(Long mecanicoId, String mensajeSiFalta) {
        if (mecanicoId == null) {
            throw new NegocioException(mensajeSiFalta);
        }
        return mecanicoRepository.findById(mecanicoId)
                .orElseThrow(() -> new NegocioException("El mecánico elegido no existe."));
    }

    /**
     * Cliente elegido en la lista, o el que ya tiene ese RUT, o uno nuevo.
     * Si es un cliente existente, se actualizan los datos que se hayan corregido
     * en el formulario (por ejemplo, un teléfono nuevo).
     */
    private Cliente resolverCliente(OrdenIngresoRequest request) {
        Optional<Cliente> existente = Optional.empty();
        if (request.getClienteId() != null) {
            existente = Optional.of(clienteRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Cliente no encontrado con ID: " + request.getClienteId())));
        } else if (request.getClienteRut() != null) {
            existente = clienteRepository.findByRut(request.getClienteRut());
        }

        if (existente.isPresent()) {
            Cliente c = existente.get();
            if (StringUtils.hasText(request.getClienteNombre()))           c.setNombre(request.getClienteNombre().trim());
            if (StringUtils.hasText(request.getClienteTelefono()))         c.setTelefono(request.getClienteTelefono().trim());
            if (StringUtils.hasText(request.getClienteRelacionVehiculo())) c.setRelacionVehiculo(request.getClienteRelacionVehiculo().trim());
            if (request.getClienteRut() != null && !request.getClienteRut().equals(c.getRut())) {
                clienteRepository.findByRut(request.getClienteRut())
                        .filter(otro -> !otro.getId().equals(c.getId()))
                        .ifPresent(otro -> {
                            throw new NegocioException("El RUT " + request.getClienteRut()
                                    + " ya pertenece al cliente " + otro.getNombre() + ".");
                        });
                c.setRut(request.getClienteRut());
            }
            return clienteRepository.save(c);
        }

        Cliente c = new Cliente();
        c.setNombre(request.getClienteNombre().trim());
        c.setTelefono(StringUtils.hasText(request.getClienteTelefono()) ? request.getClienteTelefono().trim() : null);
        c.setRut(request.getClienteRut());
        c.setRelacionVehiculo(StringUtils.hasText(request.getClienteRelacionVehiculo())
                ? request.getClienteRelacionVehiculo().trim() : null);
        return clienteRepository.save(c);
    }

    /**
     * Vehículo con esa patente (ya normalizada) o uno nuevo. Si existe, se
     * actualizan marca, modelo, año y tipo cuando vienen en el formulario.
     */
    private Vehiculo resolverVehiculo(OrdenIngresoRequest request) {
        Vehiculo v = vehiculoRepository.findByPatente(request.getVehiculoPatente())
                .orElseGet(() -> {
                    Vehiculo nuevo = new Vehiculo();
                    nuevo.setPatente(request.getVehiculoPatente());
                    log.info("Vehículo nuevo registrado: patente={}", nuevo.getPatente());
                    return nuevo;
                });
        if (StringUtils.hasText(request.getVehiculoMarca()))  v.setMarca(request.getVehiculoMarca().trim());
        if (StringUtils.hasText(request.getVehiculoModelo())) v.setModelo(request.getVehiculoModelo().trim());
        if (request.getVehiculoAnio() != null)                 v.setAnio(request.getVehiculoAnio());
        if (request.getVehiculoTipo() != null)                 v.setTipoVehiculo(request.getVehiculoTipo());
        return vehiculoRepository.save(v);
    }

    /**
     * Genera el correlativo "ORD-001" a partir de la secuencia de PostgreSQL
     * orden_correlativo_seq (atómica: dos órdenes simultáneas nunca repiten número).
     */
    private String generarCorrelativo() {
        long siguiente = ordenIngresoRepository.siguienteCorrelativo();
        return String.format("ORD-%03d", siguiente);
    }
}
