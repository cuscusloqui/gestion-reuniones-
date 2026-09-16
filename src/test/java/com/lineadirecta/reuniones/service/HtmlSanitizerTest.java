package com.lineadirecta.reuniones.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerTest {

    private HtmlSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new HtmlSanitizer();
    }

    @Test
    void eliminaEtiquetasScript() {
        String html = "<p>Hola</p><script>alert('xss')</script>";
        String resultado = sanitizer.sanear(html);

        assertThat(resultado).doesNotContain("<script");
        assertThat(resultado).doesNotContain("alert(");
        assertThat(resultado).contains("Hola");
    }

    @Test
    void preservaImagenBase64() {
        String src = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA";
        String html = "<p>Foto</p><img src=\"" + src + "\" alt=\"foto\">";
        String resultado = sanitizer.sanear(html);

        assertThat(resultado).contains("<img");
        assertThat(resultado).contains(src);
    }

    @Test
    void eliminaImagenConUrlHttp() {
        String html = "<img src=\"http://sitio-malicioso.com/rastreo.png\">";
        String resultado = sanitizer.sanear(html);

        assertThat(resultado).doesNotContain("sitio-malicioso");
        assertThat(resultado).doesNotContain("<img");
    }

    @Test
    void eliminaEnlaceConJavascript() {
        String html = "<a href=\"javascript:alert(1)\">pulsa aqui</a>";
        String resultado = sanitizer.sanear(html);

        assertThat(resultado).doesNotContain("javascript:");
        assertThat(resultado).contains("pulsa aqui");
    }

    @Test
    void preservaEnlaceHttps() {
        String html = "<a href=\"https://www.lineadirecta.com\">enlace</a>";
        String resultado = sanitizer.sanear(html);

        assertThat(resultado).contains("href=\"https://www.lineadirecta.com\"");
    }

    @Test
    void filtraPropiedadesDeEstiloNoPermitidas() {
        String html = "<p style=\"color: red; position: absolute; text-align: center;\">texto</p>";
        String resultado = sanitizer.sanear(html);

        assertThat(resultado).contains("color: red");
        assertThat(resultado).contains("text-align: center");
        assertThat(resultado).doesNotContain("position");
    }

    @Test
    void eliminaAtributosDeEventos() {
        String html = "<p onclick=\"alert(1)\">hola</p>";
        String resultado = sanitizer.sanear(html);

        assertThat(resultado).doesNotContain("onclick");
    }
}
