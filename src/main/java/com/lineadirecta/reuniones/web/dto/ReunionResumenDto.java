package com.lineadirecta.reuniones.web.dto;

import com.lineadirecta.reuniones.domain.Reunion;

import java.time.Instant;

public record ReunionResumenDto(
        Long id,
        String titulo,
        Instant fechaInicio,
        Instant fechaFin,
        String intervinientes,
        Instant fechaModificacion
) {
    public static ReunionResumenDto desde(Reunion reunion) {
        return new ReunionResumenDto(
                reunion.getId(),
                reunion.getTitulo(),
                reunion.getFechaInicio(),
                reunion.getFechaFin(),
                reunion.getIntervinientes(),
                reunion.getFechaModificacion()
        );
    }
}
