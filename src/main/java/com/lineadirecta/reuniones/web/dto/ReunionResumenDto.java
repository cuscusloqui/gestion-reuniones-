package com.lineadirecta.reuniones.web.dto;

import com.lineadirecta.reuniones.domain.Reunion;

import java.time.Instant;
import java.util.List;

public record ReunionResumenDto(
        Long id,
        String titulo,
        Instant fechaInicio,
        Instant fechaFin,
        List<String> intervinientes,
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
