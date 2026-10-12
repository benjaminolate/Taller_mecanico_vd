package com.taller.mecanico.dto;
import com.taller.mecanico.enums.TipoIngreso;
import com.taller.mecanico.enums.TipoVehiculo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
/**
 * DTO que recibe el formulario HTML de nueva orden.
 * Los campos con @NotNull/@NotBlank son validados antes de llegar al servicio.
 * En la vista se usa como th:object="${ordenRequest}".
 */
@Data
public class OrdenIngresoRequest {
    // ── Datos del Cliente ────────────────────────────────────────────────────
    // Si el cliente ya existe, se puede mandar su ID y omitir nombre/teléfono
    private Long clienteId;
    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String clienteNombre;
    private String clienteTelefono;
    private String clienteRut;
    private String clienteRelacionVehiculo;
    // ── Mecánicos ────────────────────────────────────────────────────────────
    /** Quién recibe el vehículo: obligatorio al crear la orden. */
    @NotNull(message = "Elige quién recibe el vehículo")
    private Long recibidoPorId;

    /** Quién realiza el trabajo: opcional, se asigna después (solo se usa al editar). */
    private Long mecanicoId;

    // ── Tipo de Ingreso ─────────────────────────────────────────────────────
    @NotNull(message = "Debe seleccionar un tipo de ingreso")
    private TipoIngreso tipoIngreso;
    // ── Datos del Vehículo (obligatorios cuando tipoIngreso == VEHICULO) ────
    private String vehiculoPatente;
    private String vehiculoMarca;
    private String vehiculoModelo;
    private Integer vehiculoAnio;
    private TipoVehiculo vehiculoTipo;
    // ── Estado del Vehículo ──────────────────────────────────────────────────
    private Boolean rayones      = false;
    private Boolean golpes       = false;
    private String  obsCarroceria;
    private Integer nivelCombustible;
    private Boolean nivelAceiteBajo  = false;
    private Boolean nivelAguaBajo    = false;
    private Boolean neumaticosOk     = true;
    private String  obsNeumaticos;
    private String  obsGenerales;
    // ── Estado de la Cremallera ──────────────────────────────────────────────
    private Boolean dienteDaniados = false;
    private Boolean fugasAceite    = false;
    private Boolean juegoExcesivo  = false;
    private Boolean bujesGastados  = false;
    private String  marcaCremallera;
    private Boolean esOriginal     = true;
    private String  obsCremallera;
}
