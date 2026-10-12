package com.taller.mecanico;

import com.taller.mecanico.dto.OrdenBusquedaDTO;
import com.taller.mecanico.dto.OrdenIngresoRequest;
import com.taller.mecanico.dto.TrabajoRealizadoRequest;
import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.enums.CategoriaTrabajo;
import com.taller.mecanico.enums.EstadoOrden;
import com.taller.mecanico.enums.TipoIngreso;
import com.taller.mecanico.enums.TipoVehiculo;
import com.taller.mecanico.exception.NegocioException;
import com.taller.mecanico.repository.OrdenIngresoRepository;
import com.taller.mecanico.service.OrdenIngresoService;
import com.taller.mecanico.service.TrabajoRealizadoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Flujo de una orden y sus reglas: patente y RUT, trabajos y cambios de estado.
 * Cada prueba se deshace al terminar (@Transactional).
 */
@SpringBootTest
@Transactional
class OrdenIngresoServiceTest {

    @Autowired private OrdenIngresoService     ordenService;
    @Autowired private TrabajoRealizadoService trabajoService;
    @Autowired private OrdenIngresoRepository  ordenRepo;

    @Test
    void creaOrdenDeVehiculoConCorrelativo() {
        OrdenIngreso orden = ordenService.crearOrdenCompleta(requestVehiculo("TEST01"));

        assertThat(orden.getId()).matches("ORD-\\d{3,}");
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.PENDIENTE);
        assertThat(orden.getVehiculo().getPatente()).isEqualTo("TEST01");
        assertThat(orden.getEstadoVehiculo()).isNotNull();
        assertThat(orden.getEstadoCremallera()).isNull();
    }

    @Test
    void creaOrdenDeCremalleraSinVehiculo() {
        OrdenIngresoRequest req = new OrdenIngresoRequest();
        req.setClienteNombre("Cliente Cremallera");
        req.setTipoIngreso(TipoIngreso.CREMALLERA);
        req.setRecibidoPorId(1L);
        req.setMarcaCremallera("ZF");
        req.setFugasAceite(true);

        OrdenIngreso orden = ordenService.crearOrdenCompleta(req);

        assertThat(orden.getVehiculo()).isNull();
        assertThat(orden.getEstadoCremallera()).isNotNull();
        assertThat(orden.getEstadoCremallera().getFugasAceite()).isTrue();
    }

    @Test
    void trabajosCambianElEstadoDeLaOrden() {
        OrdenIngreso orden = ordenService.crearOrdenCompleta(requestVehiculo("TEST02"));

        TrabajoRealizadoRequest t = new TrabajoRealizadoRequest();
        t.setOrdenId(orden.getId());
        t.setDescripcion("Cambio de pastillas");
        t.setCategoria(CategoriaTrabajo.REPARACION);
        t.setCostoEstimado(new BigDecimal("30000"));
        Long trabajoId = trabajoService.registrar(t).getId();

        assertThat(ordenRepo.findById(orden.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoOrden.EN_PROCESO);

        trabajoService.marcarCompletado(orden.getId(), trabajoId);

        assertThat(ordenRepo.findById(orden.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoOrden.LISTO);
        assertThat(trabajoService.calcularCostoTotal(orden.getId()))
                .isEqualByComparingTo("30000");
    }

    // ── Patente y RUT ────────────────────────────────────────────────────────

    @Test
    void patenteEnMinusculasReutilizaElVehiculoExistente() {
        OrdenIngresoRequest req = requestVehiculo("demo-01");   // DEMO01 existe en los datos de ejemplo
        req.setVehiculoMarca(null);
        req.setVehiculoModelo(null);

        OrdenIngreso orden = ordenService.crearOrdenCompleta(req);

        assertThat(orden.getVehiculo().getId()).isEqualTo(1L);
        assertThat(orden.getVehiculo().getPatente()).isEqualTo("DEMO01");
        assertThat(orden.getVehiculo().getMarca()).isEqualTo("Toyota");
    }

    @Test
    void rutConOtroFormatoReutilizaElClienteYActualizaSuTelefono() {
        OrdenIngresoRequest req = requestVehiculo("TEST03");
        req.setClienteNombre("Andrea Muestra");
        req.setClienteRut("44444444-4");                      // cliente 1 tiene "44.444.444-4"
        req.setClienteTelefono("+56911112222");

        OrdenIngreso orden = ordenService.crearOrdenCompleta(req);

        assertThat(orden.getCliente().getId()).isEqualTo(1L);
        assertThat(orden.getCliente().getTelefono()).isEqualTo("+56911112222");
    }

    @Test
    void clienteNuevoGuardaElRutFormateado() {
        OrdenIngresoRequest req = requestVehiculo("TEST04");
        req.setClienteRut("10000013k");

        OrdenIngreso orden = ordenService.crearOrdenCompleta(req);

        assertThat(orden.getCliente().getRut()).isEqualTo("10.000.013-K");
    }

    @Test
    void rechazaRutInvalido() {
        OrdenIngresoRequest req = requestVehiculo("TEST05");
        req.setClienteRut("44.444.444-0");

        assertThatThrownBy(() -> ordenService.crearOrdenCompleta(req))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no es válido");
    }

    @Test
    void rechazaIngresoDeVehiculoSinPatenteOConPatenteInvalida() {
        OrdenIngresoRequest sinPatente = requestVehiculo(null);
        assertThatThrownBy(() -> ordenService.crearOrdenCompleta(sinPatente))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("patente es obligatoria");

        OrdenIngresoRequest invalida = requestVehiculo("ABCDEF");
        assertThatThrownBy(() -> ordenService.crearOrdenCompleta(invalida))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no es válida");
    }

    @Test
    void vehiculoNuevoExigeMarcaYModelo() {
        OrdenIngresoRequest req = requestVehiculo("TEST06");
        req.setVehiculoMarca(" ");

        assertThatThrownBy(() -> ordenService.crearOrdenCompleta(req))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("marca y el modelo");
    }

    // ── Reglas de estado ─────────────────────────────────────────────────────

    @Test
    void noPermiteSaltosDeEstado() {
        OrdenIngreso orden = ordenService.crearOrdenCompleta(requestVehiculo("TEST07"));

        assertThatThrownBy(() -> trabajoService.cambiarEstadoOrden(orden.getId(), EstadoOrden.ENTREGADO))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("No se puede pasar");
    }

    @Test
    void noPermiteListoConTrabajosPendientes() {
        OrdenIngreso orden = ordenService.crearOrdenCompleta(requestVehiculo("TEST08"));
        registrarTrabajo(orden.getId(), false);              // la orden pasa a EN_PROCESO

        assertThatThrownBy(() -> trabajoService.cambiarEstadoOrden(orden.getId(), EstadoOrden.LISTO))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("pendiente");
    }

    @Test
    void agregarTrabajoAUnaOrdenListaLaReabre() {
        OrdenIngreso orden = ordenService.crearOrdenCompleta(requestVehiculo("TEST09"));
        Long t1 = registrarTrabajo(orden.getId(), false);
        trabajoService.marcarCompletado(orden.getId(), t1);
        assertThat(estadoDe(orden)).isEqualTo(EstadoOrden.LISTO);

        registrarTrabajo(orden.getId(), false);

        assertThat(estadoDe(orden)).isEqualTo(EstadoOrden.EN_PROCESO);
    }

    @Test
    void eliminarElUltimoPendienteDejaLaOrdenLista() {
        OrdenIngreso orden = ordenService.crearOrdenCompleta(requestVehiculo("TEST10"));
        Long hecho = registrarTrabajo(orden.getId(), false);
        Long pendiente = registrarTrabajo(orden.getId(), false);
        trabajoService.marcarCompletado(orden.getId(), hecho);
        assertThat(estadoDe(orden)).isEqualTo(EstadoOrden.EN_PROCESO);

        trabajoService.eliminar(orden.getId(), pendiente);

        assertThat(estadoDe(orden)).isEqualTo(EstadoOrden.LISTO);
    }

    @Test
    void ordenEntregadaNoAdmiteCambiosEnSusTrabajos() {
        OrdenIngreso orden = ordenService.crearOrdenCompleta(requestVehiculo("TEST11"));
        Long t1 = registrarTrabajo(orden.getId(), false);
        trabajoService.marcarCompletado(orden.getId(), t1);
        trabajoService.cambiarEstadoOrden(orden.getId(), EstadoOrden.ENTREGADO);

        assertThatThrownBy(() -> registrarTrabajo(orden.getId(), false))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("entregada");
        assertThatThrownBy(() -> trabajoService.eliminar(orden.getId(), t1))
                .isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> trabajoService.cambiarEstadoOrden(orden.getId(), EstadoOrden.EN_PROCESO))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void ordenEntregadaNoSePuedeEditar() {
        OrdenIngreso orden = ordenService.crearOrdenCompleta(requestVehiculo("TEST12"));
        Long t1 = registrarTrabajo(orden.getId(), false);
        trabajoService.marcarCompletado(orden.getId(), t1);
        trabajoService.cambiarEstadoOrden(orden.getId(), EstadoOrden.ENTREGADO);

        OrdenIngresoRequest edicion = ordenService.formularioEdicion(orden.getId());
        assertThatThrownBy(() -> ordenService.editarOrden(orden.getId(), edicion))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no se puede editar");
    }

    @Test
    void editarTrabajoPendienteReabreUnaOrdenLista() {
        OrdenIngreso orden = ordenService.crearOrdenCompleta(requestVehiculo("TEST13"));
        Long t1 = registrarTrabajo(orden.getId(), false);
        trabajoService.marcarCompletado(orden.getId(), t1);
        assertThat(estadoDe(orden)).isEqualTo(EstadoOrden.LISTO);

        TrabajoRealizadoRequest r = new TrabajoRealizadoRequest();
        r.setDescripcion("Trabajo de prueba");
        r.setCategoria(CategoriaTrabajo.REVISION);
        r.setCompletado(false);
        trabajoService.actualizar(orden.getId(), t1, r);

        assertThat(estadoDe(orden)).isEqualTo(EstadoOrden.EN_PROCESO);
    }

    @Test
    void quienRecibeEsObligatorioYElMecanicoSeAsignaDespues() {
        OrdenIngresoRequest sinRecibe = requestVehiculo("TEST14");
        sinRecibe.setRecibidoPorId(null);
        assertThatThrownBy(() -> ordenService.crearOrdenCompleta(sinRecibe))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("recibe");

        // Al crear, queda quién recibió y el mecánico que realiza el trabajo queda sin asignar
        OrdenIngresoRequest req = requestVehiculo("TEST15");
        req.setRecibidoPorId(2L);
        req.setMecanicoId(1L); // se ignora al crear
        OrdenIngreso orden = ordenService.crearOrdenCompleta(req);
        assertThat(orden.getRecibidoPor().getNombre()).isEqualTo("Lucía Ejemplo");
        assertThat(orden.getMecanico()).isNull();

        // Días después se asigna quién realiza el trabajo
        ordenService.asignarMecanico(orden.getId(), 1L);
        assertThat(ordenRepo.findById(orden.getId()).orElseThrow().getMecanico().getNombre()).isEqualTo("Mario Demo");

        // El historial filtrado por mecánico encuentra la orden aunque aún no tenga trabajos
        OrdenBusquedaDTO criterios = new OrdenBusquedaDTO();
        criterios.setMecanicoId(1L);
        assertThat(ordenService.buscarOrdenes(criterios, PageRequest.of(0, 50)).getContent())
                .extracting(OrdenIngreso::getId)
                .contains(orden.getId());

        // Y se puede volver a dejar sin asignar
        ordenService.asignarMecanico(orden.getId(), null);
        assertThat(ordenRepo.findById(orden.getId()).orElseThrow().getMecanico()).isNull();
    }

    private Long registrarTrabajo(String ordenId, boolean completado) {
        TrabajoRealizadoRequest t = new TrabajoRealizadoRequest();
        t.setOrdenId(ordenId);
        t.setDescripcion("Trabajo de prueba");
        t.setCategoria(CategoriaTrabajo.REVISION);
        t.setCostoEstimado(new BigDecimal("10000"));
        t.setCompletado(completado);
        return trabajoService.registrar(t).getId();
    }

    private EstadoOrden estadoDe(OrdenIngreso orden) {
        return ordenRepo.findById(orden.getId()).orElseThrow().getEstado();
    }

    private OrdenIngresoRequest requestVehiculo(String patente) {
        OrdenIngresoRequest req = new OrdenIngresoRequest();
        req.setClienteNombre("Cliente de Prueba");
        req.setClienteTelefono("+56900000000");
        req.setTipoIngreso(TipoIngreso.VEHICULO);
        req.setVehiculoPatente(patente);
        req.setVehiculoMarca("Chevrolet");
        req.setVehiculoModelo("Sail");
        req.setVehiculoAnio(2019);
        req.setVehiculoTipo(TipoVehiculo.AUTOMOVIL);
        req.setNivelCombustible(50);
        req.setRecibidoPorId(1L);
        return req;
    }
}
