package com.lineadirecta.reuniones.web.controller;

import com.lineadirecta.reuniones.domain.Reunion;
import com.lineadirecta.reuniones.service.ReunionService;
import com.lineadirecta.reuniones.web.dto.ReunionDetalleDto;
import com.lineadirecta.reuniones.web.dto.ReunionRequestDto;
import com.lineadirecta.reuniones.web.dto.ReunionResumenDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/reuniones")
public class ReunionController {

    private static final int TAMANO_PAGINA_POR_DEFECTO = 20;
    private static final int TAMANO_PAGINA_MAXIMO = 100;

    private final ReunionService service;

    public ReunionController(ReunionService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ReunionResumenDto> listar(
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta,
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String contenido,
            @RequestParam(required = false) String interviniente,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fechaInicio,desc") String sort
    ) {
        Pageable pageable = construirPageable(page, size, sort);
        return service.buscar(desde, hasta, titulo, contenido, interviniente, q, pageable)
                .map(ReunionResumenDto::desde);
    }

    @GetMapping("/rango")
    public List<ReunionResumenDto> rango(
            @RequestParam Instant desde,
            @RequestParam Instant hasta
    ) {
        return service.buscarPorRango(desde, hasta).stream()
                .map(ReunionResumenDto::desde)
                .toList();
    }

    @GetMapping("/{id}")
    public ReunionDetalleDto detalle(@PathVariable Long id) {
        return ReunionDetalleDto.desde(service.obtenerPorId(id));
    }

    @GetMapping(value = "/{id}/contenido-texto", produces = MediaType.TEXT_PLAIN_VALUE)
    public String contenidoTexto(@PathVariable Long id) {
        Reunion reunion = service.obtenerPorId(id);
        return reunion.getContenidoTexto() == null ? "" : reunion.getContenidoTexto();
    }

    @PostMapping
    public ResponseEntity<ReunionDetalleDto> crear(@Valid @RequestBody ReunionRequestDto request) {
        Reunion creada = service.crear(request);
        return ResponseEntity
                .created(URI.create("/api/reuniones/" + creada.getId()))
                .body(ReunionDetalleDto.desde(creada));
    }

    @PutMapping("/{id}")
    public ReunionDetalleDto actualizar(@PathVariable Long id, @Valid @RequestBody ReunionRequestDto request) {
        return ReunionDetalleDto.desde(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }

    private Pageable construirPageable(int page, int size, String sort) {
        int tamanoSeguro = Math.min(Math.max(size, 1), TAMANO_PAGINA_MAXIMO);
        if (tamanoSeguro <= 0) {
            tamanoSeguro = TAMANO_PAGINA_POR_DEFECTO;
        }
        Sort sortObj = parsearSort(sort);
        return PageRequest.of(Math.max(page, 0), tamanoSeguro, sortObj);
    }

    private Sort parsearSort(String sort) {
        String[] partes = sort.split(",");
        String propiedad = partes.length > 0 ? partes[0] : "fechaInicio";
        Sort.Direction direccion = (partes.length > 1 && "asc".equalsIgnoreCase(partes[1]))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direccion, propiedad);
    }
}
