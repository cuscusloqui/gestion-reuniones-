package com.lineadirecta.reuniones.web.error;

public class ReunionNoEncontradaException extends RuntimeException {

    public ReunionNoEncontradaException(Long id) {
        super("Reunion no encontrada: " + id);
    }
}
