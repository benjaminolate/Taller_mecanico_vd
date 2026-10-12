package com.taller.mecanico.exception;

/**
 * Error de una regla del taller (datos inválidos, cambio de estado no
 * permitido, etc.). Su mensaje está escrito para mostrarse tal cual al usuario.
 */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
