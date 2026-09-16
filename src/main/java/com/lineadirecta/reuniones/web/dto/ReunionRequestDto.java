package com.lineadirecta.reuniones.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record ReunionRequestDto(
        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 300, message = "El titulo no puede superar los 300 caracteres")
        String titulo,

        @NotNull(message = "La fecha de inicio es obligatoria")
        Instant fechaInicio,

        Instant fechaFin,

        List<@NotBlank(message = "Un interviniente no puede estar vacio")
             @Size(max = 200, message = "El nombre de un interviniente no puede superar los 200 caracteres")
             String> intervinientes,

        String contenidoHtml
) {
}
