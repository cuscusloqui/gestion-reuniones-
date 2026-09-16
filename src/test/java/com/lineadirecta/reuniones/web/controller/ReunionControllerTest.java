package com.lineadirecta.reuniones.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReunionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void crearReunionValidaDevuelve201ConLocation() throws Exception {
        Map<String, Object> body = Map.of(
                "titulo", "Reunion de arquitectura",
                "fechaInicio", Instant.now().toString(),
                "contenidoHtml", "<p>Notas</p>"
        );

        mockMvc.perform(post("/api/reuniones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.titulo").value("Reunion de arquitectura"));
    }

    @Test
    void crearConFechaFinAnteriorAFechaInicioDevuelve400() throws Exception {
        Instant inicio = Instant.now();
        Map<String, Object> body = Map.of(
                "titulo", "Reunion invalida",
                "fechaInicio", inicio.toString(),
                "fechaFin", inicio.minusSeconds(3600).toString()
        );

        mockMvc.perform(post("/api/reuniones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void crearSinTituloDevuelve400() throws Exception {
        Map<String, Object> body = Map.of("fechaInicio", Instant.now().toString());

        mockMvc.perform(post("/api/reuniones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearSinFechaInicioDevuelve400() throws Exception {
        Map<String, Object> body = Map.of("titulo", "Sin fecha");

        mockMvc.perform(post("/api/reuniones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rangoConHastaAnteriorADesdeDevuelve400() throws Exception {
        Instant ahora = Instant.now();
        mockMvc.perform(get("/api/reuniones/rango")
                        .param("desde", ahora.toString())
                        .param("hasta", ahora.minusSeconds(3600).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerReunionInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/reuniones/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Reunion no encontrada"));
    }

    @Test
    void obtenerContenidoTextoDevuelveTextoPlano() throws Exception {
        Long id = crearReunion("Reunion con contenido", "<p>Contenido <strong>importante</strong></p>");

        mockMvc.perform(get("/api/reuniones/" + id + "/contenido-texto"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Contenido importante"));
    }

    @Test
    void listadoConQDevuelveSoloCoincidencias() throws Exception {
        crearReunion("Reunion sobre presupuestos anuales", null);
        crearReunion("Otra reunion sin relacion", null);

        mockMvc.perform(get("/api/reuniones").param("q", "presupuestos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].titulo").value("Reunion sobre presupuestos anuales"));
    }

    @Test
    void eliminarReunionExistenteDevuelve204YLuego404() throws Exception {
        Long id = crearReunion("Reunion a eliminar", null);

        mockMvc.perform(delete("/api/reuniones/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/reuniones/" + id))
                .andExpect(status().isNotFound());
    }

    private Long crearReunion(String titulo, String contenidoHtml) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("titulo", titulo);
        body.put("fechaInicio", Instant.now().toString());
        if (contenidoHtml != null) {
            body.put("contenidoHtml", contenidoHtml);
        }

        String respuesta = mockMvc.perform(post("/api/reuniones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(respuesta).get("id").asLong();
    }
}
