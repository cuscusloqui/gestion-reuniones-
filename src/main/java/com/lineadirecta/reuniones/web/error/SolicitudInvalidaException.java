package com.lineadirecta.reuniones.web.error;

/**
 * Excepcion para errores de validacion de negocio que no cubren las anotaciones de Bean Validation
 * (por ejemplo, rangos de fechas incoherentes).
 */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
