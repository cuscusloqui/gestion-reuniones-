package com.lineadirecta.reuniones.service;

import com.lineadirecta.reuniones.domain.Reunion;
import com.lineadirecta.reuniones.repository.ReunionRepository;
import com.lineadirecta.reuniones.web.dto.ReunionRequestDto;
import com.lineadirecta.reuniones.web.error.ReunionNoEncontradaException;
import com.lineadirecta.reuniones.web.error.SolicitudInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReunionServiceTest {

    @Mock
    private ReunionRepository repository;

    private ReunionService service;

    @BeforeEach
    void setUp() {
        service = new ReunionService(repository, new HtmlSanitizer(), new TextExtractor());
    }

    @Test
    void crearGeneraContenidoTextoSinTagsNiBase64() {
        when(repository.save(any(Reunion.class))).thenAnswer(inv -> inv.getArgument(0));

        Instant inicio = Instant.now();
        ReunionRequestDto request = new ReunionRequestDto(
                "Reunion de seguimiento", inicio, null, List.of("Ana", "Luis"),
                "<p>Puntos: <strong>uno</strong></p><img src=\"data:image/png;base64,AAAA\">");

        Reunion creada = service.crear(request);

        ArgumentCaptor<Reunion> captor = ArgumentCaptor.forClass(Reunion.class);
        verify(repository).save(captor.capture());
        Reunion guardada = captor.getValue();

        assertThat(guardada.getContenidoHtml()).contains("<strong>uno</strong>");
        assertThat(guardada.getContenidoTexto()).doesNotContain("<");
        assertThat(guardada.getContenidoTexto()).doesNotContain("base64");
        assertThat(guardada.getContenidoTexto()).contains("Puntos:").contains("uno");
        assertThat(guardada.getIntervinientes()).containsExactly("Ana", "Luis");
        assertThat(creada.getTitulo()).isEqualTo("Reunion de seguimiento");
    }

    @Test
    void actualizarSustituyeIntervinientesPorLaListaCompleta() {
        when(repository.save(any(Reunion.class))).thenAnswer(inv -> inv.getArgument(0));
        Reunion existente = new Reunion("Titulo", Instant.now(), null, List.of("Ana"), null, null);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        ReunionRequestDto request = new ReunionRequestDto(
                "Titulo", Instant.now(), null, List.of("Luis", "Marta"), null);

        Reunion actualizada = service.actualizar(1L, request);

        assertThat(actualizada.getIntervinientes()).containsExactly("Luis", "Marta");
    }

    @Test
    void crearConFechaFinAnteriorAFechaInicioLanzaExcepcion() {
        Instant inicio = Instant.now();
        Instant fin = inicio.minus(1, ChronoUnit.HOURS);
        ReunionRequestDto request = new ReunionRequestDto("Titulo", inicio, fin, null, null);

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(SolicitudInvalidaException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void actualizarReunionInexistenteLanzaExcepcion() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        ReunionRequestDto request = new ReunionRequestDto("Titulo", Instant.now(), null, null, null);

        assertThatThrownBy(() -> service.actualizar(99L, request))
                .isInstanceOf(ReunionNoEncontradaException.class);
    }

    @Test
    void actualizarConFechaFinAnteriorAFechaInicioLanzaExcepcionSinTocarRepositorio() {
        Instant inicio = Instant.now();
        ReunionRequestDto request = new ReunionRequestDto("Titulo", inicio, inicio.minusSeconds(60), null, null);

        assertThatThrownBy(() -> service.actualizar(1L, request))
                .isInstanceOf(SolicitudInvalidaException.class);
        verify(repository, never()).findById(any());
    }

    @Test
    void buscarPorRangoConHastaAnteriorADesdeLanzaExcepcion() {
        Instant desde = Instant.now();
        Instant hasta = desde.minus(1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> service.buscarPorRango(desde, hasta))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void buscarPorRangoSinFechasLanzaExcepcion() {
        assertThatThrownBy(() -> service.buscarPorRango(null, Instant.now()))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void eliminarReunionInexistenteLanzaExcepcion() {
        when(repository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> service.eliminar(5L))
                .isInstanceOf(ReunionNoEncontradaException.class);
        verify(repository, never()).deleteById(any());
    }
}
