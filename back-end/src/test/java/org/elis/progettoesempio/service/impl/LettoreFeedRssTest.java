// service/impl/LettoreFeedRssTest.java
package org.elis.progettoesempio.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.elis.progettoesempio.config.NotizieProprieta.Fonte;
import org.elis.progettoesempio.dto.response.NotiziaResponse;
import org.junit.jupiter.api.Test;

class LettoreFeedRssTest {

    private final LettoreFeedRss lettore = new LettoreFeedRss();
    private final Fonte fonte = new Fonte("Testata di prova", "https://example.com/feed", "it");

    @Test
    void leggeUnFeedRssPulendoIlTestoEScartandoILinkNonValidi() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
                  <channel>
                    <title>Feed di prova</title>
                    <item>
                      <title>La BCE lascia invariati i tassi</title>
                      <link>https://example.com/articolo-1</link>
                      <description><![CDATA[<p>Il consiglio direttivo &amp; i mercati <b>reagiscono</b>.</p>]]></description>
                      <pubDate>Tue, 13 Oct 2026 10:30:00 GMT</pubDate>
                      <media:thumbnail width="240" height="135" url="https://example.com/img1.jpg"/>
                    </item>
                    <item>
                      <title>Link non valido</title>
                      <link>javascript:alert(1)</link>
                      <description>da scartare</description>
                    </item>
                  </channel>
                </rss>
                """;

        List<NotiziaResponse> notizie = lettore.leggi(fonte, xml.getBytes(StandardCharsets.UTF_8));

        assertThat(notizie).hasSize(1);
        NotiziaResponse notizia = notizie.get(0);
        assertThat(notizia.titolo()).isEqualTo("La BCE lascia invariati i tassi");
        assertThat(notizia.sintesi()).isEqualTo("Il consiglio direttivo & i mercati reagiscono.");
        assertThat(notizia.url()).isEqualTo("https://example.com/articolo-1");
        assertThat(notizia.fonte()).isEqualTo("Testata di prova");
        assertThat(notizia.lingua()).isEqualTo("it");
        assertThat(notizia.immagineUrl()).isEqualTo("https://example.com/img1.jpg");
        assertThat(notizia.pubblicataIl()).isEqualTo(Instant.parse("2026-10-13T10:30:00Z"));
    }

    @Test
    void leggeUnFeedAtom() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <feed xmlns="http://www.w3.org/2005/Atom">
                  <title>Atom di prova</title>
                  <entry>
                    <title>Inflazione in calo</title>
                    <link rel="alternate" href="https://example.org/inflazione"/>
                    <summary>Dati Eurostat</summary>
                    <updated>2026-10-13T08:00:00+02:00</updated>
                  </entry>
                </feed>
                """;

        List<NotiziaResponse> notizie = lettore.leggi(fonte, xml.getBytes(StandardCharsets.UTF_8));

        assertThat(notizie).hasSize(1);
        assertThat(notizie.get(0).titolo()).isEqualTo("Inflazione in calo");
        assertThat(notizie.get(0).url()).isEqualTo("https://example.org/inflazione");
        assertThat(notizie.get(0).pubblicataIl()).isEqualTo(Instant.parse("2026-10-13T06:00:00Z"));
    }

    @Test
    void ilFeedConUnaDichiarazioneDoctypeVieneRifiutato() {
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE rss [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <rss version="2.0"><channel><item>
                  <title>&xxe;</title><link>https://example.com/x</link>
                </item></channel></rss>
                """;

        assertThatThrownBy(() -> lettore.leggi(fonte, xml.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unaSintesiTroppoLungaVieneTroncata() {
        String descrizione = "parola ".repeat(100);
        String xml = """
                <rss version="2.0"><channel><item>
                  <title>Titolo</title><link>https://example.com/lungo</link>
                  <description>%s</description>
                </item></channel></rss>
                """.formatted(descrizione);

        List<NotiziaResponse> notizie = lettore.leggi(fonte, xml.getBytes(StandardCharsets.UTF_8));

        assertThat(notizie.get(0).sintesi()).hasSizeLessThanOrEqualTo(240).endsWith("...");
    }
}