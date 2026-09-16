package com.lineadirecta.reuniones.service;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/**
 * Genera la version en texto plano del contenido HTML sanitizado, sin etiquetas ni datos base64,
 * para permitir busquedas por palabra sin escanear el HTML.
 */
@Component
public class TextExtractor {

    public String extraerTexto(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.parse(html).text().trim();
    }
}
