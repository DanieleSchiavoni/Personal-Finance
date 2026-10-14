// service/impl/NotizieServiceImplTest.java
package org.elis.progettoesempio.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.elis.progettoesempio.config.NotizieProprieta;
import org.elis.progettoesempio.config.NotizieProprieta.Fonte;
import org.elis.progettoesempio.dto.response.NotiziaResponse;
import org.elis.progettoesempio.service.ScaricatoreFeed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotizieServiceImplTest {

    private static final Fonte FONTE_A = new Fonte("Testata A", "https://a.example.com/feed", "en");
    private static final Fonte FONTE_B = new Fonte("Testata B", "https://b.example.com/feed", "it");

    private final LettoreFeedRss lettore = new LettoreFeedRss();
    private final OrologioModificabile orologio = new OrologioModificabile();
    private final AtomicInteger scaricamenti = new AtomicInteger();

    /** Contenuto XML per indirizzo; un indirizzo assente simula una fonte che non risponde */
    private Map<String, String> feed;

    @BeforeEach
    void preparaDati() {
        feed = Map.of(
                FONTE_A.url(), rss(voce("A alle 10", "https://a.example.com/1", "Tue, 13 Oct 2026 10:00:00 GMT"),
                        voce("A alle 08", "https://a.example.com/2", "Tue, 13 Oct 2026 08:00:00 GMT")),
                FONTE_B.url(), rss(voce("B alle 09", "https://b.example.com/1", "Tue, 13 Oct 2026 09:00:00 GMT")));
    }

    @Test
    void leNotizieDiPiuFontiSonoOrdinateDallaPiuRecente() {
        NotizieServiceImpl servizio = creaServizio(true, 15, feed, FONTE_A, FONTE_B);

        List<NotiziaResponse> notizie = servizio.ultime(10);

        assertThat(notizie).extracting(NotiziaResponse::titolo).containsExactly("A alle 10", "B alle 09", "A alle 08");
    }

    @Test
    void unaFonteNonDisponibileNonBloccaLeAltre() {
        Map<String, String> soloA = Map.of(FONTE_A.url(), feed.get(FONTE_A.url()));
        NotizieServiceImpl servizio = creaServizio(true, 15, soloA, FONTE_A, FONTE_B);

        assertThat(servizio.ultime(10)).extracting(NotiziaResponse::titolo).containsExactly("A alle 10", "A alle 08");
    }

    @Test
    void laCacheEvitaNuoviDownloadFinoAllaScadenza() {
        NotizieServiceImpl servizio = creaServizio(true, 15, feed, FONTE_A, FONTE_B);

        servizio.ultime(10);
        assertThat(scaricamenti.get()).isEqualTo(2);

        orologio.avanza(Duration.ofMinutes(10));
        servizio.ultime(10);
        assertThat(scaricamenti.get()).isEqualTo(2); // ancora in cache

        orologio.avanza(Duration.ofMinutes(6));
        servizio.ultime(10);
        assertThat(scaricamenti.get()).isEqualTo(4); // scaduta: si riscaricano entrambe
    }

    @Test
    void seTutteLeFontiFallisconoSiUsaLUltimaCache() {
        // Il contenuto è una variabile che il test può svuotare tra una chiamata e l'altra
        Map<String, String> modificabile = new java.util.concurrent.ConcurrentHashMap<>(feed);
        NotizieServiceImpl servizio = creaServizio(true, 15, modificabile, FONTE_A, FONTE_B);
        assertThat(servizio.ultime(10)).hasSize(3);

        modificabile.clear();
        orologio.avanza(Duration.ofMinutes(20));

        assertThat(servizio.ultime(10)).hasSize(3);
    }

    @Test
    void ilLimiteVieneRispettatoEPortatoAUnMinimoDiUno() {
        NotizieServiceImpl servizio = creaServizio(true, 15, feed, FONTE_A, FONTE_B);

        assertThat(servizio.ultime(2)).hasSize(2);
        assertThat(servizio.ultime(0)).hasSize(1);
    }

    @Test
    void lArticoloPresenteInPiuFeedCompareUnaSolaVolta() {
        Map<String, String> doppioni = Map.of(
                FONTE_A.url(), rss(voce("Stesso articolo", "https://example.com/uguale", "Tue, 13 Oct 2026 10:00:00 GMT")),
                FONTE_B.url(), rss(voce("Stesso articolo", "https://example.com/uguale", "Tue, 13 Oct 2026 10:00:00 GMT")));
        NotizieServiceImpl servizio = creaServizio(true, 15, doppioni, FONTE_A, FONTE_B);

        assertThat(servizio.ultime(10)).hasSize(1);
    }

    @Test
    void conLeNotizieDisabilitateNonSiScaricaNulla() {
        NotizieServiceImpl servizio = creaServizio(false, 15, feed, FONTE_A, FONTE_B);

        assertThat(servizio.ultime(10)).isEmpty();
        assertThat(scaricamenti.get()).isZero();
    }

    private NotizieServiceImpl creaServizio(boolean abilitato, int minutiCache, Map<String, String> contenuti,
            Fonte... fonti) {
        ScaricatoreFeed scaricatore = url -> {
            scaricamenti.incrementAndGet();
            String xml = contenuti.get(url);
            if (xml == null) {
                throw new IOException("Fonte non raggiungibile: " + url);
            }
            return xml.getBytes(StandardCharsets.UTF_8);
        };
        NotizieProprieta proprieta = new NotizieProprieta(abilitato, minutiCache, List.of(fonti));
        return new NotizieServiceImpl(proprieta, scaricatore, lettore, orologio);
    }

    private static String voce(String titolo, String url, String data) {
        return "<item><title>" + titolo + "</title><link>" + url + "</link><pubDate>" + data + "</pubDate></item>";
    }

    private static String rss(String... voci) {
        return "<rss version=\"2.0\"><channel>" + String.join("", voci) + "</channel></rss>";
    }

    /** Un orologio che il test può far avanzare */
    private static class OrologioModificabile extends Clock {
        private Instant adesso = Instant.parse("2026-10-13T12:00:00Z");

        void avanza(Duration durata) {
            adesso = adesso.plus(durata);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return adesso;
        }
    }
}