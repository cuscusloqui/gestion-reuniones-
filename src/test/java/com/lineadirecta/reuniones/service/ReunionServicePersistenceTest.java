package com.lineadirecta.reuniones.service;

import com.lineadirecta.reuniones.domain.Reunion;
import com.lineadirecta.reuniones.repository.ReunionRepository;
import com.lineadirecta.reuniones.web.dto.ReunionRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas que requieren el ciclo de vida real de JPA (fechaCreacion/fechaModificacion via
 * @PrePersist/@PreUpdate) y las Specifications de busqueda combinada contra una base H2 en memoria.
 */
@DataJpaTest
class ReunionServicePersistenceTest {

    @Autowired
    private ReunionRepository repository;

    private ReunionService service;

    @BeforeEach
    void setUp() {
        service = new ReunionService(repository, new HtmlSanitizer(), new TextExtractor());
    }

    @Test
    void crearPersisteYRellenaFechasDeAuditoria() {
        ReunionRequestDto request = new ReunionRequestDto("Kickoff proyecto", Instant.now(), null, null, "<p>notas</p>");

        Reunion creada = service.crear(request);

        assertThat(creada.getId()).isNotNull();
        assertThat(creada.getFechaCreacion()).isNotNull();
        assertThat(creada.getFechaModificacion()).isEqualTo(creada.getFechaCreacion());
    }

    @Test
    void actualizarCambiaFechaModificacionPeroNoFechaCreacion() throws InterruptedException {
        Reunion creada = service.crear(new ReunionRequestDto("Titulo original", Instant.now(), null, null, null));
        repository.flush();
        Instant fechaCreacionOriginal = creada.getFechaCreacion();

        Thread.sleep(20);

        ReunionRequestDto actualizacion = new ReunionRequestDto("Titulo modificado", Instant.now(), null, null, null);
        Reunion actualizada = service.actualizar(creada.getId(), actualizacion);
        repository.flush();

        assertThat(actualizada.getFechaCreacion()).isEqualTo(fechaCreacionOriginal);
        assertThat(actualizada.getFechaModificacion()).isAfter(fechaCreacionOriginal);
        assertThat(actualizada.getTitulo()).isEqualTo("Titulo modificado");
    }

    @Test
    void buscarSoloConDesdeFiltraPorFecha() {
        Instant ahora = Instant.now();
        crear("Antigua", ahora.minus(10, ChronoUnit.DAYS), null, null);
        crear("Reciente", ahora, null, null);

        var pagina = service.buscar(ahora.minus(1, ChronoUnit.DAYS), null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(pagina.getContent()).extracting(Reunion::getTitulo).containsExactly("Reciente");
    }

    @Test
    void buscarSoloConTituloFiltraPorTitulo() {
        Instant ahora = Instant.now();
        crear("Sprint planning", ahora, null, null);
        crear("Retrospectiva", ahora, null, null);

        var pagina = service.buscar(null, null, "sprint", null, null, null, PageRequest.of(0, 10));

        assertThat(pagina.getContent()).extracting(Reunion::getTitulo).containsExactly("Sprint planning");
    }

    @Test
    void buscarConTituloDesdeYHastaCombinaConAnd() {
        Instant ahora = Instant.now();
        crear("Sprint planning", ahora, null, null);
        crear("Sprint planning", ahora.minus(30, ChronoUnit.DAYS), null, null);

        var pagina = service.buscar(
                ahora.minus(1, ChronoUnit.DAYS), ahora.plus(1, ChronoUnit.DAYS),
                "sprint", null, null, null, PageRequest.of(0, 10));

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).getFechaInicio()).isEqualTo(ahora);
    }

    @Test
    void buscarConQHaceOrEntreTituloContenidoEIntervinientes() {
        Instant ahora = Instant.now();
        crear("Reunion presupuesto", ahora, null, "<p>hablamos de marketing</p>");
        crear("Otra reunion", ahora, "Equipo de marketing", null);
        crear("Sin relacion", ahora, null, null);

        var pagina = service.buscar(null, null, null, null, null, "marketing", PageRequest.of(0, 10));

        assertThat(pagina.getContent()).extracting(Reunion::getTitulo)
                .containsExactlyInAnyOrder("Reunion presupuesto", "Otra reunion");
    }

    @Test
    void buscarConQEIntervinienteCombinaConAnd() {
        Instant ahora = Instant.now();
        crear("Reunion A", ahora, "Carlos, Marketing", null);
        crear("Reunion marketing", ahora, "Solo Ana", null);

        var pagina = service.buscar(null, null, null, null, "carlos", "marketing", PageRequest.of(0, 10));

        assertThat(pagina.getContent()).extracting(Reunion::getTitulo).containsExactly("Reunion A");
    }

    private void crear(String titulo, Instant fechaInicio, String intervinientes, String contenidoHtml) {
        service.crear(new ReunionRequestDto(titulo, fechaInicio, null, intervinientes, contenidoHtml));
    }
}
