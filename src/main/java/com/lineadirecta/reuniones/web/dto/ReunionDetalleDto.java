package com.lineadirecta.reuniones.web.dto;

import com.lineadirecta.reuniones.domain.Reunion;

import java.time.Instant;
import java.util.List;

public record ReunionDetalleDto(
        Long id,
        String titulo,
        Instant fechaInicio,
        Instant fechaFin,
        List<String> intervinientes,
        String contenidoHtml,
        String contenidoTexto,
        Instant fechaCreacion,
        Instant fechaModificacion
) {
    public static ReunionDetalleDto desde(Reunion reunion) {
        return new ReunionDetalleDto(
                reunion.getId(),
                reunion.getTitulo(),
                reunion.getFechaInicio(),
                reunion.getFechaFin(),
                reunion.getIntervinientes(),
                reunion.getContenidoHtml(),
                reunion.getContenidoTexto(),
                reunion.getFechaCreacion(),
                reunion.getFechaModificacion()
        );
    }
}
