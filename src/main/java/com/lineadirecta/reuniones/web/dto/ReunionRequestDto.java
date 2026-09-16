package com.lineadirecta.reuniones.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ReunionRequestDto(
        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 300, message = "El titulo no puede superar los 300 caracteres")
        String titulo,

        @NotNull(message = "La fecha de inicio es obligatoria")
        Instant fechaInicio,

        Instant fechaFin,

        @Size(max = 2000, message = "Los intervinientes no pueden superar los 2000 caracteres")
        String intervinientes,

        String contenidoHtml
) {
}
