package com.lineadirecta.reuniones.web.error;

import java.util.List;

public record ErrorResponse(String error, List<String> detalles) {

    public ErrorResponse(String error) {
        this(error, List.of());
    }
}
