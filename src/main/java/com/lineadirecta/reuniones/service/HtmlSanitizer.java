package com.lineadirecta.reuniones.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Sanea el HTML del editor rich-text antes de persistirlo, evitando XSS.
 * Ultima linea de defensa: no confiamos en que el navegador limite lo que se puede enviar.
 */
@Component
public class HtmlSanitizer {

    private static final Pattern SRC_IMAGEN_PERMITIDA =
            Pattern.compile("^data:image/(png|jpeg|jpg|gif|webp);base64,[A-Za-z0-9+/=]+$", Pattern.CASE_INSENSITIVE);

    private static final Set<String> PROPIEDADES_ESTILO_PERMITIDAS = Set.of(
            "color", "background-color", "font-weight", "font-style", "text-decoration", "text-align");

    private final Safelist safelist = construirSafelist();

    private static Safelist construirSafelist() {
        return new Safelist()
                .addTags(
                        "p", "br", "hr", "h1", "h2", "h3", "h4", "h5", "h6",
                        "strong", "b", "em", "i", "u", "s",
                        "ul", "ol", "li", "blockquote", "pre", "code",
                        "span", "div", "a", "img",
                        "table", "thead", "tbody", "tr", "th", "td")
                .addAttributes("a", "href")
                .addAttributes("img", "src", "alt", "title")
                .addAttributes(":all", "style")
                .addProtocols("a", "href", "http", "https", "mailto")
                .addProtocols("img", "src", "data");
    }

    public String sanear(String htmlOriginal) {
        if (htmlOriginal == null || htmlOriginal.isBlank()) {
            return "";
        }
        String limpio = Jsoup.clean(htmlOriginal, "", safelist);
        return postProcesar(limpio);
    }

    private String postProcesar(String html) {
        Document doc = Jsoup.parseBodyFragment(html);
        doc.outputSettings().prettyPrint(false);

        for (Element img : doc.select("img")) {
            if (!SRC_IMAGEN_PERMITIDA.matcher(img.attr("src")).matches()) {
                img.remove();
            }
        }

        for (Element elemento : doc.select("[style]")) {
            String estiloFiltrado = filtrarEstilo(elemento.attr("style"));
            if (estiloFiltrado.isBlank()) {
                elemento.removeAttr("style");
            } else {
                elemento.attr("style", estiloFiltrado);
            }
        }

        return doc.body().html();
    }

    private String filtrarEstilo(String estilo) {
        StringBuilder resultado = new StringBuilder();
        for (String declaracion : estilo.split(";")) {
            String[] partes = declaracion.split(":", 2);
            if (partes.length != 2) {
                continue;
            }
            String propiedad = partes[0].trim().toLowerCase();
            String valor = partes[1].trim();
            if (PROPIEDADES_ESTILO_PERMITIDAS.contains(propiedad) && !valor.isBlank()) {
                resultado.append(propiedad).append(": ").append(valor).append("; ");
            }
        }
        return resultado.toString().trim();
    }
}
