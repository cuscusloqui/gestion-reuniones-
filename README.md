# Gestión de Reuniones

Aplicación web monolítica y autocontenida para gestionar reuniones sobre un calendario, con
editor rich-text para las notas y una API REST completa para consultarlas desde otras
herramientas (incluido Claude Code).

- **Backend**: Java 21 + Spring Boot 3 (Web, Data JPA, Validation).
- **Persistencia**: H2 en modo fichero (`./data/reuniones.mv.db`). Los datos sobreviven a reinicios.
- **Frontend**: HTML + CSS + JS vanilla servido desde `src/main/resources/static/`. Sin
  frameworks, sin build tools, sin CDNs. Funciona offline tras el primer arranque.
- **Sanitización**: todo el HTML del editor se sanea en el backend con Jsoup antes de persistir
  (`HtmlSanitizer`), evitando XSS.

## Arranque

```bash
./mvnw spring-boot:run
```

> Si la descarga del wrapper de Maven falla en tu entorno (por ejemplo, por restricciones de
> antivirus/red corporativa al mover la distribución descargada), usa el Maven instalado en el
> sistema, que es equivalente:
>
> ```bash
> mvn spring-boot:run
> ```

La aplicación arranca en `http://localhost:8081/gestion-reuniones` y muestra el calendario del mes actual.

### Scripts de Windows

Para uso diario sin depender de Maven, hay dos `.bat` en la raíz del proyecto (requieren el JAR
ya construido; lo compilan solos si falta):

```bat
arrancar.bat
parar.bat
```

`arrancar.bat` compila si hace falta, lanza la app en una ventana minimizada, espera a que
responda y abre... bueno, te dice la URL para que la abras tú. Si la aplicación se cae nada más
arrancar, vuelca el log (`gestion-reuniones.log`) en la misma consola en vez de dejar una ventana
en blanco que se cierra sola. `parar.bat` la detiene localizándola por el título de su ventana.

## Construir el JAR ejecutable

```bash
./mvnw clean package
java -jar target/gestion-reuniones.jar
```

(o `mvn clean package` si usas el Maven del sistema).

## Dónde se guardan los datos

En `./data/reuniones.mv.db` (fichero H2), relativo al directorio desde el que se lanza la
aplicación. La carpeta `data/` está en `.gitignore`.

## Tests

```bash
./mvnw test
# o
mvn test
```

## API REST

Base path: `/api/reuniones`. CORS abierto (`*`) para uso desde cualquier herramienta local.

### Crear una reunión

```bash
curl -X POST http://localhost:8081/gestion-reuniones/api/reuniones \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Reunión de arquitectura",
    "fechaInicio": "2026-09-20T09:00:00Z",
    "fechaFin": "2026-09-20T10:00:00Z",
    "intervinientes": ["Carlos", "Ana"],
    "contenidoHtml": "<p>Puntos a tratar: <strong>diseño de la API</strong></p>"
  }'
```

Devuelve `201 Created` con cabecera `Location: /api/reuniones/{id}`.

### Actualizar una reunión

```bash
curl -X PUT http://localhost:8081/gestion-reuniones/api/reuniones/1 \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Reunión de arquitectura (revisada)",
    "fechaInicio": "2026-09-20T09:00:00Z",
    "fechaFin": "2026-09-20T10:30:00Z",
    "intervinientes": ["Carlos", "Luis"],
    "contenidoHtml": "<p>Contenido actualizado</p>"
  }'
```

> `intervinientes` es una lista de nombres (array JSON), no un texto libre. Cada actualización
> envía la lista completa deseada: para añadir un participante, inclúyelo en el array; para
> quitarlo, simplemente omítelo. No hay endpoints separados de alta/baja de participantes: se
> guarda de forma explícita, como el resto de la reunión.

### Listar con filtros combinados

Todos los parámetros son opcionales y se combinan con AND (salvo `q`, que hace OR entre título,
contenido e intervinientes):

```bash
# Por rango de fechas
curl "http://localhost:8081/gestion-reuniones/api/reuniones?desde=2026-09-01T00:00:00Z&hasta=2026-09-30T23:59:59Z"

# Por título
curl "http://localhost:8081/gestion-reuniones/api/reuniones?titulo=arquitectura"

# Combinado: título + rango de fechas + paginación
curl "http://localhost:8081/gestion-reuniones/api/reuniones?titulo=arquitectura&desde=2026-09-01T00:00:00Z&hasta=2026-09-30T23:59:59Z&page=0&size=10"

# Búsqueda global (OR entre título, contenido e intervinientes)
curl "http://localhost:8081/gestion-reuniones/api/reuniones?q=presupuesto"

# Búsqueda global + interviniente (AND)
curl "http://localhost:8081/gestion-reuniones/api/reuniones?q=presupuesto&interviniente=carlos"
```

### Obtener el detalle completo

```bash
curl http://localhost:8081/gestion-reuniones/api/reuniones/1
```

### Obtener solo el contenido en texto plano

Útil para que Claude Code (u otra herramienta) lea las notas sin lidiar con HTML:

```bash
curl http://localhost:8081/gestion-reuniones/api/reuniones/1/contenido-texto
```

### Reuniones en un rango de fechas (para pintar el calendario)

```bash
curl "http://localhost:8081/gestion-reuniones/api/reuniones/rango?desde=2026-09-01T00:00:00Z&hasta=2026-09-30T23:59:59Z"
```

### Eliminar

```bash
curl -X DELETE http://localhost:8081/gestion-reuniones/api/reuniones/1
```

## Uso desde Claude Code

- Para saber qué reuniones hay en un periodo (por ejemplo, "esta semana"), usa
  `GET /api/reuniones/rango?desde=...&hasta=...` con fechas ISO 8601 en UTC.
- Para buscar reuniones por texto (título, contenido o intervinientes), usa
  `GET /api/reuniones?q=<texto>` y pagina con `page`/`size` si hay muchos resultados.
- Para leer el contenido de una reunión concreta sin HTML ni imágenes en base64, usa
  `GET /api/reuniones/{id}/contenido-texto` (devuelve `text/plain`).
- El detalle completo (`GET /api/reuniones/{id}`) incluye `contenidoHtml`, útil solo si necesitas
  el formato original; para la mayoría de análisis de texto basta con `contenidoTexto`.

## Estructura del proyecto

```
src/main/java/.../reuniones/
  domain/         Entidad JPA Reunion
  repository/     Spring Data JPA + Specifications de busqueda
  service/        ReunionService, HtmlSanitizer, TextExtractor
  web/
    controller/   ReunionController (REST)
    dto/          DTOs de request/response
    error/        RestControllerAdvice + excepciones de dominio
  config/         CORS
src/main/resources/
  static/         index.html, app.js, styles.css (frontend)
  application.yml
data/             Fichero H2 (no versionado)
```
