# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

## [0.1.0] - 2026-09-16

### Added

- Aplicación inicial de gestión de reuniones: backend Spring Boot 3 (Web, Data JPA, Validation)
  con persistencia H2 en fichero.
- Entidad `Reunion` con título, fechas de inicio/fin, intervinientes y contenido rich-text
  (HTML sanitizado + texto plano extraído automáticamente).
- Sanitización de HTML con Jsoup (`HtmlSanitizer`) para prevenir XSS: whitelist de etiquetas,
  atributos y propiedades de estilo; solo imágenes `data:image/...` en base64.
- API REST bajo `/api/reuniones`: CRUD completo, búsqueda combinada por Specifications
  (fecha, título, contenido, intervinientes, búsqueda global `q`), endpoint de rango para el
  calendario y endpoint de contenido en texto plano.
- Frontend vanilla (HTML/CSS/JS) con vistas de calendario Mes/Semana/Día, editor rich-text
  propio basado en `contenteditable`, y modal de búsqueda avanzada.
- Suite de tests: unitarios de sanitización y servicio, `@DataJpaTest` para búsqueda combinada
  y persistencia, `@SpringBootTest` + MockMvc para los endpoints REST.
