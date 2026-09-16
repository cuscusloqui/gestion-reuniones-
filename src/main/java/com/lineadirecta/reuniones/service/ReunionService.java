package com.lineadirecta.reuniones.service;

import com.lineadirecta.reuniones.domain.Reunion;
import com.lineadirecta.reuniones.repository.ReunionRepository;
import com.lineadirecta.reuniones.repository.ReunionSpecifications;
import com.lineadirecta.reuniones.web.dto.ReunionRequestDto;
import com.lineadirecta.reuniones.web.error.ReunionNoEncontradaException;
import com.lineadirecta.reuniones.web.error.SolicitudInvalidaException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class ReunionService {

    private final ReunionRepository repository;
    private final HtmlSanitizer htmlSanitizer;
    private final TextExtractor textExtractor;

    public ReunionService(ReunionRepository repository, HtmlSanitizer htmlSanitizer, TextExtractor textExtractor) {
        this.repository = repository;
        this.htmlSanitizer = htmlSanitizer;
        this.textExtractor = textExtractor;
    }

    public Reunion crear(ReunionRequestDto request) {
        validarFechas(request.fechaInicio(), request.fechaFin());
        String htmlSaneado = htmlSanitizer.sanear(request.contenidoHtml());
        String texto = textExtractor.extraerTexto(htmlSaneado);

        Reunion reunion = new Reunion(
                request.titulo(),
                request.fechaInicio(),
                request.fechaFin(),
                request.intervinientes(),
                htmlSaneado,
                texto
        );
        return repository.save(reunion);
    }

    public Reunion actualizar(Long id, ReunionRequestDto request) {
        validarFechas(request.fechaInicio(), request.fechaFin());
        Reunion reunion = obtenerPorId(id);

        String htmlSaneado = htmlSanitizer.sanear(request.contenidoHtml());
        String texto = textExtractor.extraerTexto(htmlSaneado);

        reunion.setTitulo(request.titulo());
        reunion.setFechaInicio(request.fechaInicio());
        reunion.setFechaFin(request.fechaFin());
        reunion.setIntervinientes(request.intervinientes());
        reunion.setContenidoHtml(htmlSaneado);
        reunion.setContenidoTexto(texto);

        return repository.save(reunion);
    }

    @Transactional(readOnly = true)
    public Reunion obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ReunionNoEncontradaException(id));
    }

    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ReunionNoEncontradaException(id);
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<Reunion> buscar(Instant desde, Instant hasta, String titulo, String contenido,
                                 String interviniente, String q, Pageable pageable) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new SolicitudInvalidaException("El parametro 'hasta' no puede ser anterior a 'desde'");
        }
        Specification<Reunion> spec = ReunionSpecifications.combinar(
                ReunionSpecifications.desde(desde),
                ReunionSpecifications.hasta(hasta),
                ReunionSpecifications.tituloContiene(titulo),
                ReunionSpecifications.contenidoContiene(contenido),
                ReunionSpecifications.intervinienteContiene(interviniente),
                ReunionSpecifications.global(q)
        );
        return repository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<Reunion> buscarPorRango(Instant desde, Instant hasta) {
        if (desde == null || hasta == null) {
            throw new SolicitudInvalidaException("Los parametros 'desde' y 'hasta' son obligatorios");
        }
        if (hasta.isBefore(desde)) {
            throw new SolicitudInvalidaException("El parametro 'hasta' no puede ser anterior a 'desde'");
        }
        Specification<Reunion> spec = ReunionSpecifications.combinar(
                ReunionSpecifications.desde(desde),
                ReunionSpecifications.hasta(hasta)
        );
        return repository.findAll(spec);
    }

    private void validarFechas(Instant fechaInicio, Instant fechaFin) {
        if (fechaFin != null && fechaInicio != null && fechaFin.isBefore(fechaInicio)) {
            throw new SolicitudInvalidaException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
    }
}
