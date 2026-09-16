// static/app.js
// Aplicacion de calendario de reuniones. JS vanilla, sin dependencias externas.
(function () {
  "use strict";

  const API_BASE = "/api/reuniones";
  const DIAS_SEMANA = ["L", "M", "X", "J", "V", "S", "D"];
  const HORAS_DIA = Array.from({ length: 24 }, (_, i) => i);
  const ALTURA_HORA_PX = 52;

  const estado = {
    vista: "month",
    fechaActual: new Date(),
    edicionId: null,
    editorSucio: false,
    lastRange: null,
    busqueda: { page: 0, size: 10, totalPages: 0, filtros: {} },
  };

  // ---------- Utilidades de fecha ----------

  function inicioDeSemana(fecha) {
    const d = new Date(fecha);
    const dia = (d.getDay() + 6) % 7; // 0 = lunes
    d.setHours(0, 0, 0, 0);
    d.setDate(d.getDate() - dia);
    return d;
  }

  function sumarDias(fecha, n) {
    const d = new Date(fecha);
    d.setDate(d.getDate() + n);
    return d;
  }

  function mismaFechaLocal(a, b) {
    return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
  }

  function dosDigitos(n) {
    return String(n).padStart(2, "0");
  }

  function aValorInputLocal(fecha) {
    return (
      fecha.getFullYear() +
      "-" +
      dosDigitos(fecha.getMonth() + 1) +
      "-" +
      dosDigitos(fecha.getDate()) +
      "T" +
      dosDigitos(fecha.getHours()) +
      ":" +
      dosDigitos(fecha.getMinutes())
    );
  }

  function isoDesdeInputLocal(valor) {
    if (!valor) return null;
    const fecha = new Date(valor);
    return fecha.toISOString();
  }

  function fechaDesdeIso(iso) {
    return new Date(iso);
  }

  const formatoMes = new Intl.DateTimeFormat("es-ES", { month: "long", year: "numeric" });
  const formatoDiaCompleto = new Intl.DateTimeFormat("es-ES", { weekday: "long", day: "numeric", month: "long", year: "numeric" });
  const formatoDiaCorto = new Intl.DateTimeFormat("es-ES", { weekday: "short", day: "numeric" });
  const formatoHora = new Intl.DateTimeFormat("es-ES", { hour: "2-digit", minute: "2-digit", hour12: false });
  const formatoFechaHora = new Intl.DateTimeFormat("es-ES", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit", hour12: false });

  // ---------- Cliente API ----------

  async function apiFetch(url, opciones) {
    const respuesta = await fetch(url, opciones);
    if (!respuesta.ok) {
      let mensaje = "Error en la peticion";
      try {
        const cuerpo = await respuesta.json();
        mensaje = cuerpo.error || mensaje;
      } catch (e) {
        // sin cuerpo JSON
      }
      throw new Error(mensaje);
    }
    if (respuesta.status === 204) return null;
    const contentType = respuesta.headers.get("content-type") || "";
    if (contentType.includes("application/json")) return respuesta.json();
    return respuesta.text();
  }

  function obtenerRango(desde, hasta) {
    const params = new URLSearchParams({ desde: desde.toISOString(), hasta: hasta.toISOString() });
    return apiFetch(`${API_BASE}/rango?${params}`);
  }

  function obtenerDetalle(id) {
    return apiFetch(`${API_BASE}/${id}`);
  }

  function obtenerContenidoTexto(id) {
    return apiFetch(`${API_BASE}/${id}/contenido-texto`);
  }

  function crearReunion(payload) {
    return apiFetch(API_BASE, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
  }

  function actualizarReunion(id, payload) {
    return apiFetch(`${API_BASE}/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
  }

  function eliminarReunion(id) {
    return apiFetch(`${API_BASE}/${id}`, { method: "DELETE" });
  }

  function buscarReuniones(filtros, page, size) {
    const params = new URLSearchParams();
    Object.entries(filtros).forEach(([clave, valor]) => {
      if (valor) params.set(clave, valor);
    });
    params.set("page", page);
    params.set("size", size);
    return apiFetch(`${API_BASE}?${params}`);
  }

  // ---------- Toast ----------

  let toastTimeout = null;
  function mostrarToast(texto) {
    const toast = document.getElementById("toast");
    toast.textContent = texto;
    toast.hidden = false;
    clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => { toast.hidden = true; }, 2200);
  }

  // ---------- Cabecera / navegacion ----------

  function actualizarEtiquetaRango() {
    const etiqueta = document.getElementById("range-label");
    if (estado.vista === "month") {
      etiqueta.textContent = formatoMes.format(estado.fechaActual);
    } else if (estado.vista === "week") {
      const inicio = inicioDeSemana(estado.fechaActual);
      const fin = sumarDias(inicio, 6);
      etiqueta.textContent = `Sem. ${inicio.getDate()} ${formatoMes.format(inicio).split(" ")[0]} — ${fin.getDate()} ${formatoMes.format(fin)}`;
    } else {
      etiqueta.textContent = formatoDiaCompleto.format(estado.fechaActual);
    }
  }

  function cambiarVista(vista) {
    estado.vista = vista;
    document.querySelectorAll(".view-tab").forEach((tab) => {
      tab.setAttribute("aria-selected", String(tab.dataset.view === vista));
    });
    document.getElementById("month-view").hidden = vista !== "month";
    document.getElementById("week-view").hidden = vista !== "week";
    document.getElementById("day-view").hidden = vista !== "day";
    renderizarVistaActual();
  }

  function navegar(delta) {
    const d = new Date(estado.fechaActual);
    if (estado.vista === "month") d.setMonth(d.getMonth() + delta);
    else if (estado.vista === "week") d.setDate(d.getDate() + delta * 7);
    else d.setDate(d.getDate() + delta);
    estado.fechaActual = d;
    renderizarVistaActual();
  }

  function irAHoy() {
    estado.fechaActual = new Date();
    renderizarVistaActual();
  }

  function renderizarVistaActual() {
    actualizarEtiquetaRango();
    if (estado.vista === "month") renderizarMes();
    else if (estado.vista === "week") renderizarSemana();
    else renderizarDia();
  }

  // ---------- Vista mensual ----------

  async function renderizarMes() {
    const contenedor = document.getElementById("month-view");
    const primerDiaMes = new Date(estado.fechaActual.getFullYear(), estado.fechaActual.getMonth(), 1);
    const ultimoDiaMes = new Date(estado.fechaActual.getFullYear(), estado.fechaActual.getMonth() + 1, 0);
    const inicioGrid = inicioDeSemana(primerDiaMes);
    const finGrid = sumarDias(inicioDeSemana(ultimoDiaMes), 6);
    const numDias = Math.round((finGrid - inicioGrid) / 86400000) + 1;

    let reuniones = [];
    try {
      reuniones = await obtenerRango(inicioGrid, sumarDias(finGrid, 1));
    } catch (e) {
      mostrarToast("Error al cargar reuniones: " + e.message);
    }

    const cabecera = document.createElement("div");
    cabecera.className = "month-grid-header";
    DIAS_SEMANA.forEach((d) => {
      const div = document.createElement("div");
      div.textContent = d;
      cabecera.appendChild(div);
    });

    const grid = document.createElement("div");
    grid.className = "month-grid";
    const hoy = new Date();

    for (let i = 0; i < numDias; i++) {
      const fechaCelda = sumarDias(inicioGrid, i);
      const celda = document.createElement("div");
      celda.className = "month-cell";
      if (fechaCelda.getMonth() !== estado.fechaActual.getMonth()) celda.classList.add("otro-mes");
      if (mismaFechaLocal(fechaCelda, hoy)) celda.classList.add("hoy");

      const numero = document.createElement("div");
      numero.className = "day-number";
      numero.textContent = fechaCelda.getDate();
      celda.appendChild(numero);

      const reunionesDelDia = reuniones
        .filter((r) => mismaFechaLocal(fechaDesdeIso(r.fechaInicio), fechaCelda))
        .sort((a, b) => new Date(a.fechaInicio) - new Date(b.fechaInicio));

      reunionesDelDia.slice(0, 3).forEach((r) => {
        const chip = document.createElement("div");
        chip.className = "chip";
        chip.textContent = r.titulo;
        chip.title = r.titulo;
        chip.addEventListener("click", (ev) => { ev.stopPropagation(); abrirDetalle(r.id); });
        celda.appendChild(chip);
      });

      if (reunionesDelDia.length > 3) {
        const mas = document.createElement("div");
        mas.className = "chip-mas";
        mas.textContent = `+${reunionesDelDia.length - 3} más`;
        mas.addEventListener("click", (ev) => {
          ev.stopPropagation();
          mostrarPopoverMas(mas, reunionesDelDia);
        });
        celda.appendChild(mas);
      }

      celda.addEventListener("click", () => abrirEditor(null, fechaCelda));
      grid.appendChild(celda);
    }

    contenedor.innerHTML = "";
    contenedor.appendChild(cabecera);
    contenedor.appendChild(grid);
  }

  function mostrarPopoverMas(anclaje, reuniones) {
    const popover = document.getElementById("popover-mas");
    popover.innerHTML = "";
    reuniones.forEach((r) => {
      const chip = document.createElement("div");
      chip.className = "chip";
      chip.textContent = r.titulo;
      chip.addEventListener("click", () => { ocultarPopoverMas(); abrirDetalle(r.id); });
      popover.appendChild(chip);
    });
    const rect = anclaje.getBoundingClientRect();
    popover.style.top = `${rect.bottom + 4}px`;
    popover.style.left = `${rect.left}px`;
    popover.hidden = false;
  }

  function ocultarPopoverMas() {
    document.getElementById("popover-mas").hidden = true;
  }

  document.addEventListener("click", (ev) => {
    const popover = document.getElementById("popover-mas");
    if (!popover.hidden && !popover.contains(ev.target) && !ev.target.classList.contains("chip-mas")) {
      ocultarPopoverMas();
    }
  });

  // ---------- Vistas semanal / diaria (grid horario) ----------

  function construirTimeGrid(dias) {
    const grid = document.createElement("div");
    grid.className = "time-grid";

    const cabecera = document.createElement("div");
    cabecera.className = "time-grid-header";
    cabecera.style.gridTemplateColumns = `56px repeat(${dias.length}, 1fr)`;
    const gutterCabecera = document.createElement("div");
    gutterCabecera.className = "gutter";
    cabecera.appendChild(gutterCabecera);

    const hoy = new Date();
    dias.forEach((dia) => {
      const div = document.createElement("div");
      div.className = "day-col-header";
      if (mismaFechaLocal(dia, hoy)) div.classList.add("hoy");
      const dow = document.createElement("div");
      dow.className = "dow";
      dow.textContent = formatoDiaCorto.format(dia).split(" ")[0].replace(".", "");
      const dnum = document.createElement("div");
      dnum.className = "dnum";
      dnum.textContent = dia.getDate();
      div.appendChild(dow);
      div.appendChild(dnum);
      cabecera.appendChild(div);
    });

    const cuerpo = document.createElement("div");
    cuerpo.className = "time-body";
    cuerpo.style.gridTemplateColumns = `56px repeat(${dias.length}, 1fr)`;
    cuerpo.style.display = "grid";

    const gutter = document.createElement("div");
    gutter.className = "gutter time-rows";
    HORAS_DIA.forEach((h) => {
      const fila = document.createElement("div");
      fila.className = "hour-row";
      const span = document.createElement("span");
      span.textContent = `${dosDigitos(h)}:00`;
      fila.appendChild(span);
      gutter.appendChild(fila);
    });
    cuerpo.appendChild(gutter);

    const columnas = dias.map((dia) => {
      const col = document.createElement("div");
      col.className = "day-col time-rows";
      HORAS_DIA.forEach((h) => {
        const fila = document.createElement("div");
        fila.className = "hour-row";
        fila.addEventListener("click", () => {
          const fecha = new Date(dia);
          fecha.setHours(h, 0, 0, 0);
          abrirEditor(null, fecha);
        });
        col.appendChild(fila);
      });
      cuerpo.appendChild(col);
      return col;
    });

    grid.appendChild(cabecera);
    grid.appendChild(cuerpo);
    return { grid, columnas };
  }

  function calcularLayoutSolapes(eventos) {
    // Asigna columna/total a cada evento para pintarlos lado a lado si se solapan.
    const ordenados = [...eventos].sort((a, b) => a.inicio - b.inicio);
    const grupos = [];
    let grupoActual = [];
    let finMaximo = null;

    ordenados.forEach((ev) => {
      if (grupoActual.length === 0 || ev.inicio < finMaximo) {
        grupoActual.push(ev);
        finMaximo = finMaximo === null ? ev.fin : new Date(Math.max(finMaximo, ev.fin));
      } else {
        grupos.push(grupoActual);
        grupoActual = [ev];
        finMaximo = ev.fin;
      }
    });
    if (grupoActual.length > 0) grupos.push(grupoActual);

    grupos.forEach((grupo) => {
      grupo.forEach((ev, idx) => {
        ev.columna = idx;
        ev.totalColumnas = grupo.length;
      });
    });
    return ordenados;
  }

  function pintarEventosEnColumna(col, dia, reuniones) {
    const eventosDia = reuniones
      .filter((r) => mismaFechaLocal(fechaDesdeIso(r.fechaInicio), dia))
      .map((r) => {
        const inicio = fechaDesdeIso(r.fechaInicio);
        const fin = r.fechaFin ? fechaDesdeIso(r.fechaFin) : new Date(inicio.getTime() + 60 * 60000);
        return { r, inicio, fin };
      });

    calcularLayoutSolapes(eventosDia).forEach((ev) => {
      const minutosInicio = ev.inicio.getHours() * 60 + ev.inicio.getMinutes();
      const duracionMin = Math.max((ev.fin - ev.inicio) / 60000, 20);
      const bloque = document.createElement("div");
      bloque.className = "evento-bloque";
      bloque.style.top = `${(minutosInicio / 60) * ALTURA_HORA_PX}px`;
      bloque.style.height = `${(duracionMin / 60) * ALTURA_HORA_PX}px`;
      const anchoPorc = 100 / ev.totalColumnas;
      bloque.style.width = `calc(${anchoPorc}% - 4px)`;
      bloque.style.left = `${ev.columna * anchoPorc}%`;

      const titulo = document.createElement("span");
      titulo.className = "ev-titulo";
      titulo.textContent = ev.r.titulo;
      const hora = document.createElement("span");
      hora.className = "ev-hora";
      hora.textContent = formatoHora.format(ev.inicio) + (ev.r.fechaFin ? " - " + formatoHora.format(ev.fin) : "");

      bloque.appendChild(titulo);
      bloque.appendChild(hora);
      bloque.addEventListener("click", (e) => { e.stopPropagation(); abrirDetalle(ev.r.id); });
      col.appendChild(bloque);
    });
  }

  async function renderizarSemana() {
    const contenedor = document.getElementById("week-view");
    const inicio = inicioDeSemana(estado.fechaActual);
    const dias = Array.from({ length: 7 }, (_, i) => sumarDias(inicio, i));

    let reuniones = [];
    try {
      reuniones = await obtenerRango(inicio, sumarDias(inicio, 7));
    } catch (e) {
      mostrarToast("Error al cargar reuniones: " + e.message);
    }

    const { grid, columnas } = construirTimeGrid(dias);
    dias.forEach((dia, idx) => pintarEventosEnColumna(columnas[idx], dia, reuniones));

    contenedor.innerHTML = "";
    contenedor.appendChild(grid);
  }

  async function renderizarDia() {
    const contenedor = document.getElementById("day-view");
    const dia = new Date(estado.fechaActual);
    dia.setHours(0, 0, 0, 0);

    let reuniones = [];
    try {
      reuniones = await obtenerRango(dia, sumarDias(dia, 1));
    } catch (e) {
      mostrarToast("Error al cargar reuniones: " + e.message);
    }

    const { grid, columnas } = construirTimeGrid([dia]);
    pintarEventosEnColumna(columnas[0], dia, reuniones);

    contenedor.innerHTML = "";
    contenedor.appendChild(grid);
  }

  // ---------- Editor rich-text ----------

  const richEditor = () => document.getElementById("rich-editor");

  function inicializarEditorRichText() {
    try { document.execCommand("styleWithCSS", false, true); } catch (e) { /* no soportado */ }

    document.getElementById("editor-toolbar").addEventListener("click", (ev) => {
      const boton = ev.target.closest(".tb-btn");
      if (!boton) return;
      ev.preventDefault();
      restaurarSeleccion();
      const cmd = boton.dataset.cmd;
      if (!cmd) return;
      const valor = boton.dataset.value || null;
      richEditor().focus();
      document.execCommand(cmd, false, valor);
      marcarEditorSucio();
    });

    document.getElementById("tb-heading").addEventListener("change", (ev) => {
      restaurarSeleccion();
      richEditor().focus();
      document.execCommand("formatBlock", false, ev.target.value);
      marcarEditorSucio();
    });

    document.getElementById("tb-forecolor").addEventListener("input", (ev) => {
      restaurarSeleccion();
      richEditor().focus();
      document.execCommand("foreColor", false, ev.target.value);
      marcarEditorSucio();
    });

    document.getElementById("tb-backcolor").addEventListener("input", (ev) => {
      restaurarSeleccion();
      richEditor().focus();
      document.execCommand("backColor", false, ev.target.value);
      marcarEditorSucio();
    });

    ["mouseup", "keyup"].forEach((evento) => {
      richEditor().addEventListener(evento, guardarSeleccion);
    });
    richEditor().addEventListener("input", marcarEditorSucio);

    // Enlace
    document.getElementById("tb-link").addEventListener("click", (ev) => {
      ev.preventDefault();
      guardarSeleccion();
      document.getElementById("link-popover").hidden = false;
      document.getElementById("link-url").value = "";
      document.getElementById("link-url").focus();
    });
    document.getElementById("link-cancelar").addEventListener("click", () => {
      document.getElementById("link-popover").hidden = true;
    });
    document.getElementById("link-aceptar").addEventListener("click", () => {
      const url = document.getElementById("link-url").value.trim();
      document.getElementById("link-popover").hidden = true;
      if (!url) return;
      restaurarSeleccion();
      richEditor().focus();
      document.execCommand("createLink", false, url);
      marcarEditorSucio();
    });

    // Imagen
    document.getElementById("tb-image").addEventListener("click", (ev) => {
      ev.preventDefault();
      guardarSeleccion();
      document.getElementById("tb-image-input").click();
    });
    document.getElementById("tb-image-input").addEventListener("change", manejarSeleccionImagen);
  }

  function guardarSeleccion() {
    const sel = window.getSelection();
    if (sel && sel.rangeCount > 0 && richEditor().contains(sel.anchorNode)) {
      estado.lastRange = sel.getRangeAt(0).cloneRange();
    }
  }

  function restaurarSeleccion() {
    if (!estado.lastRange) return;
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(estado.lastRange);
  }

  function marcarEditorSucio() {
    estado.editorSucio = true;
  }

  const MAX_LADO_IMAGEN = 1600;

  function manejarSeleccionImagen(ev) {
    const archivo = ev.target.files[0];
    ev.target.value = "";
    if (!archivo) return;

    const lector = new FileReader();
    lector.onload = () => {
      const img = new Image();
      img.onload = () => {
        const dataUrl = redimensionarImagen(img, archivo.type);
        restaurarSeleccion();
        richEditor().focus();
        document.execCommand("insertImage", false, dataUrl);
        marcarEditorSucio();
      };
      img.src = lector.result;
    };
    lector.readAsDataURL(archivo);
  }

  function redimensionarImagen(img, tipoOriginal) {
    let { width, height } = img;
    if (width > MAX_LADO_IMAGEN || height > MAX_LADO_IMAGEN) {
      const escala = MAX_LADO_IMAGEN / Math.max(width, height);
      width = Math.round(width * escala);
      height = Math.round(height * escala);
    }
    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;
    canvas.getContext("2d").drawImage(img, 0, 0, width, height);

    const tipoSalida = ["image/png", "image/jpeg", "image/webp"].includes(tipoOriginal) ? tipoOriginal : "image/png";
    return canvas.toDataURL(tipoSalida, 0.85);
  }

  // ---------- Modal editor de reunion ----------

  function abrirEditor(reunion, fechaPreseleccionada) {
    estado.edicionId = reunion ? reunion.id : null;
    estado.editorSucio = false;
    estado.lastRange = null;

    document.getElementById("editor-titulo-modal").textContent = reunion ? "Editar reunión" : "Nueva reunión";
    document.getElementById("btn-eliminar").hidden = !reunion;
    document.getElementById("input-titulo").value = reunion ? reunion.titulo : "";
    document.getElementById("input-intervinientes").value = reunion ? (reunion.intervinientes || "") : "";
    richEditor().innerHTML = reunion ? (reunion.contenidoHtml || "") : "";

    const inicio = reunion ? fechaDesdeIso(reunion.fechaInicio) : (fechaPreseleccionada || new Date());
    document.getElementById("input-inicio").value = aValorInputLocal(inicio);
    document.getElementById("input-fin").value = reunion && reunion.fechaFin ? aValorInputLocal(fechaDesdeIso(reunion.fechaFin)) : "";

    document.getElementById("link-popover").hidden = true;
    document.getElementById("modal-editor").hidden = false;
    document.getElementById("input-titulo").focus();
  }

  function hayDatosEnEditor() {
    return (
      document.getElementById("input-titulo").value.trim() !== "" ||
      document.getElementById("input-intervinientes").value.trim() !== "" ||
      richEditor().innerHTML.trim() !== ""
    );
  }

  function cerrarEditor() {
    if (estado.editorSucio && hayDatosEnEditor()) {
      if (!confirm("Hay cambios sin guardar. ¿Cerrar de todas formas?")) return;
    }
    document.getElementById("modal-editor").hidden = true;
  }

  async function guardarReunion() {
    const titulo = document.getElementById("input-titulo").value.trim();
    const inicioValor = document.getElementById("input-inicio").value;
    const finValor = document.getElementById("input-fin").value;

    if (!titulo) { mostrarToast("El título es obligatorio"); return; }
    if (!inicioValor) { mostrarToast("La fecha de inicio es obligatoria"); return; }

    const payload = {
      titulo,
      fechaInicio: isoDesdeInputLocal(inicioValor),
      fechaFin: finValor ? isoDesdeInputLocal(finValor) : null,
      intervinientes: document.getElementById("input-intervinientes").value.trim() || null,
      contenidoHtml: richEditor().innerHTML,
    };

    try {
      if (estado.edicionId) {
        await actualizarReunion(estado.edicionId, payload);
      } else {
        await crearReunion(payload);
      }
      estado.editorSucio = false;
      document.getElementById("modal-editor").hidden = true;
      mostrarToast("Guardado ✓");
      renderizarVistaActual();
    } catch (e) {
      mostrarToast("Error al guardar: " + e.message);
    }
  }

  async function eliminarReunionActual() {
    if (!estado.edicionId) return;
    if (!confirm("¿Seguro que quieres eliminar esta reunión?")) return;
    try {
      await eliminarReunion(estado.edicionId);
      document.getElementById("modal-editor").hidden = true;
      document.getElementById("modal-detalle").hidden = true;
      mostrarToast("Reunión eliminada");
      renderizarVistaActual();
    } catch (e) {
      mostrarToast("Error al eliminar: " + e.message);
    }
  }

  // ---------- Modal detalle ----------

  let detalleActualId = null;

  async function abrirDetalle(id) {
    try {
      const reunion = await obtenerDetalle(id);
      detalleActualId = id;
      document.getElementById("detalle-titulo").textContent = reunion.titulo;
      const rangoFechas = formatoFechaHora.format(fechaDesdeIso(reunion.fechaInicio)) +
        (reunion.fechaFin ? " — " + formatoFechaHora.format(fechaDesdeIso(reunion.fechaFin)) : "");
      document.getElementById("detalle-fechas").textContent = rangoFechas;

      const wrap = document.getElementById("detalle-intervinientes-wrap");
      if (reunion.intervinientes) {
        wrap.hidden = false;
        document.getElementById("detalle-intervinientes").textContent = reunion.intervinientes;
      } else {
        wrap.hidden = true;
      }

      document.getElementById("detalle-contenido").innerHTML = reunion.contenidoHtml || "<em>Sin contenido</em>";
      document.getElementById("modal-detalle").hidden = false;
    } catch (e) {
      mostrarToast("Error al abrir la reunión: " + e.message);
    }
  }

  async function editarDesdeDetalle() {
    if (!detalleActualId) return;
    try {
      const reunion = await obtenerDetalle(detalleActualId);
      document.getElementById("modal-detalle").hidden = true;
      abrirEditor(reunion);
    } catch (e) {
      mostrarToast("Error al cargar la reunión: " + e.message);
    }
  }

  // ---------- Modal busqueda ----------

  function abrirBusqueda() {
    document.getElementById("modal-busqueda").hidden = false;
  }

  function cerrarBusqueda() {
    document.getElementById("modal-busqueda").hidden = true;
  }

  function limpiarBusqueda() {
    ["buscar-desde", "buscar-hasta", "buscar-titulo", "buscar-contenido", "buscar-interviniente", "buscar-q"].forEach((id) => {
      document.getElementById(id).value = "";
    });
    document.getElementById("resultados-busqueda").innerHTML = "";
    document.getElementById("paginacion-busqueda").hidden = true;
  }

  function recogerFiltrosBusqueda() {
    const desde = document.getElementById("buscar-desde").value;
    const hasta = document.getElementById("buscar-hasta").value;
    return {
      desde: desde ? isoDesdeInputLocal(desde) : null,
      hasta: hasta ? isoDesdeInputLocal(hasta) : null,
      titulo: document.getElementById("buscar-titulo").value.trim() || null,
      contenido: document.getElementById("buscar-contenido").value.trim() || null,
      interviniente: document.getElementById("buscar-interviniente").value.trim() || null,
      q: document.getElementById("buscar-q").value.trim() || null,
    };
  }

  function escaparHtml(texto) {
    const div = document.createElement("div");
    div.textContent = texto;
    return div.innerHTML;
  }

  function resaltarYRecortar(textoPlano, termino) {
    if (!termino) return "";
    const indice = textoPlano.toLowerCase().indexOf(termino.toLowerCase());
    if (indice === -1) return "";
    const inicio = Math.max(0, indice - 40);
    const fin = Math.min(textoPlano.length, indice + termino.length + 60);
    let fragmento = textoPlano.substring(inicio, fin);
    if (inicio > 0) fragmento = "…" + fragmento;
    if (fin < textoPlano.length) fragmento += "…";

    const escapado = escaparHtml(fragmento);
    const regex = new RegExp(escaparRegex(escaparHtml(termino)), "ig");
    return escapado.replace(regex, (m) => `<mark>${m}</mark>`);
  }

  function escaparRegex(texto) {
    return texto.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  }

  async function ejecutarBusqueda(page) {
    const filtros = recogerFiltrosBusqueda();
    estado.busqueda.filtros = filtros;
    estado.busqueda.page = page || 0;

    try {
      const resultado = await buscarReuniones(filtros, estado.busqueda.page, estado.busqueda.size);
      estado.busqueda.totalPages = resultado.totalPages;
      await renderizarResultadosBusqueda(resultado.content, filtros);
      actualizarPaginacionBusqueda(resultado);
    } catch (e) {
      mostrarToast("Error en la búsqueda: " + e.message);
    }
  }

  async function renderizarResultadosBusqueda(resultados, filtros) {
    const contenedor = document.getElementById("resultados-busqueda");
    contenedor.innerHTML = "";

    if (resultados.length === 0) {
      contenedor.innerHTML = "<p>No se han encontrado reuniones.</p>";
      return;
    }

    const terminoFragmento = filtros.contenido || filtros.q;

    for (const r of resultados) {
      const item = document.createElement("div");
      item.className = "resultado-item";

      const titulo = document.createElement("div");
      titulo.className = "r-titulo";
      titulo.textContent = r.titulo;

      const meta = document.createElement("div");
      meta.className = "r-meta";
      meta.textContent = formatoFechaHora.format(fechaDesdeIso(r.fechaInicio)) + (r.intervinientes ? " · " + r.intervinientes : "");

      const fragmento = document.createElement("div");
      fragmento.className = "r-fragmento";

      item.appendChild(titulo);
      item.appendChild(meta);
      item.appendChild(fragmento);
      item.addEventListener("click", () => { cerrarBusqueda(); abrirDetalle(r.id); });
      contenedor.appendChild(item);

      if (terminoFragmento) {
        obtenerContenidoTexto(r.id).then((texto) => {
          fragmento.innerHTML = resaltarYRecortar(texto || "", terminoFragmento);
        }).catch(() => {});
      }
    }
  }

  function actualizarPaginacionBusqueda(resultado) {
    const paginacion = document.getElementById("paginacion-busqueda");
    if (resultado.totalPages <= 1) {
      paginacion.hidden = true;
      return;
    }
    paginacion.hidden = false;
    document.getElementById("pag-info").textContent = `${resultado.number + 1} / ${resultado.totalPages}`;
    document.getElementById("pag-anterior").disabled = resultado.first;
    document.getElementById("pag-siguiente").disabled = resultado.last;
  }

  // ---------- Inicializacion / listeners ----------

  function inicializar() {
    document.querySelectorAll(".view-tab").forEach((tab) => {
      tab.addEventListener("click", () => cambiarVista(tab.dataset.view));
    });
    document.getElementById("btn-prev").addEventListener("click", () => navegar(-1));
    document.getElementById("btn-next").addEventListener("click", () => navegar(1));
    document.getElementById("btn-today").addEventListener("click", irAHoy);

    document.getElementById("btn-new").addEventListener("click", () => abrirEditor(null, new Date()));
    document.getElementById("btn-cerrar-editor").addEventListener("click", cerrarEditor);
    document.getElementById("btn-cerrar-footer").addEventListener("click", cerrarEditor);
    document.getElementById("btn-guardar").addEventListener("click", guardarReunion);
    document.getElementById("btn-eliminar").addEventListener("click", eliminarReunionActual);

    document.getElementById("btn-cerrar-detalle").addEventListener("click", () => { document.getElementById("modal-detalle").hidden = true; });
    document.getElementById("btn-cerrar-detalle-footer").addEventListener("click", () => { document.getElementById("modal-detalle").hidden = true; });
    document.getElementById("btn-editar-desde-detalle").addEventListener("click", editarDesdeDetalle);

    document.getElementById("btn-search").addEventListener("click", abrirBusqueda);
    document.getElementById("btn-cerrar-busqueda").addEventListener("click", cerrarBusqueda);
    document.getElementById("btn-limpiar-busqueda").addEventListener("click", limpiarBusqueda);
    document.getElementById("btn-ejecutar-busqueda").addEventListener("click", () => ejecutarBusqueda(0));
    document.getElementById("pag-anterior").addEventListener("click", () => ejecutarBusqueda(estado.busqueda.page - 1));
    document.getElementById("pag-siguiente").addEventListener("click", () => ejecutarBusqueda(estado.busqueda.page + 1));

    inicializarEditorRichText();
    cambiarVista("month");
  }

  document.addEventListener("DOMContentLoaded", inicializar);
})();
