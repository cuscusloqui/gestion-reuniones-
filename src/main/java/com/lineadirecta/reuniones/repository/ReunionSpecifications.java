package com.lineadirecta.reuniones.repository;

import com.lineadirecta.reuniones.domain.Reunion;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/**
 * Construye Specifications combinables (AND) para el filtrado de reuniones.
 * Cada filtro solo se aplica si el valor informado no es nulo/vacio.
 */
public final class ReunionSpecifications {

    private ReunionSpecifications() {
    }

    public static Specification<Reunion> desde(Instant desde) {
        if (desde == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaInicio"), desde);
    }

    public static Specification<Reunion> hasta(Instant hasta) {
        if (hasta == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaInicio"), hasta);
    }

    public static Specification<Reunion> tituloContiene(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return null;
        }
        String patron = "%" + titulo.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("titulo")), patron);
    }

    public static Specification<Reunion> contenidoContiene(String contenido) {
        if (contenido == null || contenido.isBlank()) {
            return null;
        }
        String patron = "%" + contenido.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("contenidoTexto")), patron);
    }

    public static Specification<Reunion> intervinienteContiene(String interviniente) {
        if (interviniente == null || interviniente.isBlank()) {
            return null;
        }
        String patron = "%" + interviniente.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("intervinientes")), patron);
    }

    /**
     * Busqueda global: OR entre titulo, contenidoTexto e intervinientes.
     */
    public static Specification<Reunion> global(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String patron = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate porTitulo = cb.like(cb.lower(root.get("titulo")), patron);
            Predicate porContenido = cb.like(cb.lower(root.get("contenidoTexto")), patron);
            Predicate porIntervinientes = cb.like(cb.lower(root.get("intervinientes")), patron);
            return cb.or(porTitulo, porContenido, porIntervinientes);
        };
    }

    public static Specification<Reunion> combinar(Specification<Reunion>... specs) {
        Specification<Reunion> resultado = Specification.where(null);
        for (Specification<Reunion> spec : specs) {
            if (spec != null) {
                resultado = resultado.and(spec);
            }
        }
        return resultado;
    }
}
