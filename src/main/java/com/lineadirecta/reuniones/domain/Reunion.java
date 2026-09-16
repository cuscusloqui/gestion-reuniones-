package com.lineadirecta.reuniones.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entidad JPA que representa una reunion. No se expone directamente en la API: la frontera usa DTOs.
 */
@Entity
@Table(name = "reunion", indexes = {
        @Index(name = "idx_reunion_fecha_inicio", columnList = "fechaInicio"),
        @Index(name = "idx_reunion_fecha_inicio_fin", columnList = "fechaInicio, fechaFin")
})
public class Reunion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String titulo;

    @Column(nullable = false)
    private Instant fechaInicio;

    private Instant fechaFin;

    @Column(length = 2000)
    private String intervinientes;

    @Column(columnDefinition = "CLOB")
    private String contenidoHtml;

    @Column(columnDefinition = "CLOB")
    private String contenidoTexto;

    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(nullable = false)
    private Instant fechaModificacion;

    protected Reunion() {
        // Requerido por JPA
    }

    public Reunion(String titulo, Instant fechaInicio, Instant fechaFin, String intervinientes,
                    String contenidoHtml, String contenidoTexto) {
        this.titulo = titulo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.intervinientes = intervinientes;
        this.contenidoHtml = contenidoHtml;
        this.contenidoTexto = contenidoTexto;
    }

    @PrePersist
    protected void alCrear() {
        Instant ahora = Instant.now();
        this.fechaCreacion = ahora;
        this.fechaModificacion = ahora;
    }

    @PreUpdate
    protected void alActualizar() {
        this.fechaModificacion = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Instant getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(Instant fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Instant getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(Instant fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getIntervinientes() {
        return intervinientes;
    }

    public void setIntervinientes(String intervinientes) {
        this.intervinientes = intervinientes;
    }

    public String getContenidoHtml() {
        return contenidoHtml;
    }

    public void setContenidoHtml(String contenidoHtml) {
        this.contenidoHtml = contenidoHtml;
    }

    public String getContenidoTexto() {
        return contenidoTexto;
    }

    public void setContenidoTexto(String contenidoTexto) {
        this.contenidoTexto = contenidoTexto;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Instant getFechaModificacion() {
        return fechaModificacion;
    }
}
