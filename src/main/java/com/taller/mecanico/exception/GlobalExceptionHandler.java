package com.taller.mecanico.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Muestra una página de error entendible en vez de la página técnica de Spring.
 * Los errores inesperados se registran en el log con todo el detalle, pero al
 * usuario solo se le muestra un mensaje general.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(EntityNotFoundException ex, Model model) {
        model.addAttribute("titulo",  "No encontrado");
        model.addAttribute("mensaje", ex.getMessage());
        return "error/error";
    }

    // Archivo o dirección inexistente (por ejemplo /favicon.ico): 404 sin ensuciar el log.
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleSinRecurso(NoResourceFoundException ex, Model model) {
        model.addAttribute("titulo",  "Página no encontrada");
        model.addAttribute("mensaje", "La dirección que intentas abrir no existe.");
        return "error/error";
    }

    // Acción reservada para otro rol (por ejemplo, un empleado en /usuarios)
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleSinPermiso(AccessDeniedException ex, Model model) {
        model.addAttribute("titulo",  "Sin permiso");
        model.addAttribute("mensaje", "Esta acción está reservada para el jefe del taller.");
        return "error/error";
    }

    @ExceptionHandler(NegocioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleNegocio(NegocioException ex, Model model) {
        model.addAttribute("titulo",  "Operación no permitida");
        model.addAttribute("mensaje", ex.getMessage());
        return "error/error";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleParametroInvalido(MethodArgumentTypeMismatchException ex, Model model) {
        model.addAttribute("titulo",  "Datos incorrectos");
        model.addAttribute("mensaje", "Uno de los datos enviados no tiene el formato esperado.");
        return "error/error";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleIntegridad(DataIntegrityViolationException ex, Model model) {
        log.warn("Violación de integridad de datos", ex);
        model.addAttribute("titulo",  "No se pudo guardar");
        model.addAttribute("mensaje",
                "Los datos chocan con un registro existente (por ejemplo, una patente o un RUT repetido).");
        return "error/error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, Model model) {
        log.error("Error inesperado", ex);
        model.addAttribute("titulo",  "Error inesperado");
        model.addAttribute("mensaje",
                "Ocurrió un error inesperado. Intenta nuevamente y, si se repite, avisa al administrador.");
        return "error/error";
    }
}
