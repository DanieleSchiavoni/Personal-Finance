// service/impl/LettoreFeedRss.java
package org.elis.progettoesempio.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.elis.progettoesempio.config.NotizieProprieta.Fonte;
import org.elis.progettoesempio.dto.response.NotiziaResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Trasforma il contenuto XML di un feed (RSS 2.0 o Atom) in un elenco di notizie. Non fa chiamate di rete. */
@Component
public class LettoreFeedRss {

    private static final int LUNGHEZZA_MASSIMA_SINTESI = 240;

    /** @throws IllegalArgumentException se il contenuto non è un XML valido o non è ammesso */
    public List<NotiziaResponse> leggi(Fonte fonte, byte[] contenuto) {
        Document documento = analizza(contenuto, fonte);
        List<NotiziaResponse> notizie = new ArrayList<>();

        NodeList voci = documento.getElementsByTagName("item"); // RSS
        for (int i = 0; i < voci.getLength(); i++) {
            creaNotizia(fonte, leggiVoceRss((Element) voci.item(i))).ifPresent(notizie::add);
        }
        NodeList elementi = documento.getElementsByTagName("entry"); // Atom
        for (int i = 0; i < elementi.getLength(); i++) {
            creaNotizia(fonte, leggiVoceAtom((Element) elementi.item(i))).ifPresent(notizie::add);
        }
        return notizie;
    }

    /** Il lettore XML rifiuta DOCTYPE ed entità esterne: protegge dagli attacchi XXE */
    private Document analizza(byte[] contenuto, Fonte fonte) {
        try {
            DocumentBuilderFactory fabbrica = DocumentBuilderFactory.newInstance();
            fabbrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabbrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabbrica.setXIncludeAware(false);
            fabbrica.setExpandEntityReferences(false);
            return fabbrica.newDocumentBuilder().parse(new ByteArrayInputStream(contenuto));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("Feed non valido (" + fonte.nome() + "): " + e.getMessage(), e);
        }
    }

    private VoceGrezza leggiVoceRss(Element voce) {
        String link = testo(voce, "link");
        if (link == null || link.isBlank()) {
            link = testo(voce, "guid");
        }
        String data = testo(voce, "pubDate");
        if (data == null) {
            data = testo(voce, "dc:date");
        }
        return new VoceGrezza(testo(voce, "title"), link, testo(voce, "description"), data, immagine(voce));
    }

    private VoceGrezza leggiVoceAtom(Element voce) {
        String link = null;
        NodeList collegamenti = voce.getElementsByTagName("link");
        for (int i = 0; i < collegamenti.getLength(); i++) {
            Element collegamento = (Element) collegamenti.item(i);
            String rel = collegamento.getAttribute("rel");
            if (rel.isEmpty() || "alternate".equals(rel)) {
                link = collegamento.getAttribute("href");
                break;
            }
        }
        String descrizione = testo(voce, "summary");
        if (descrizione == null) {
            descrizione = testo(voce, "content");
        }
        String data = testo(voce, "published");
        if (data == null) {
            data = testo(voce, "updated");
        }
        return new VoceGrezza(testo(voce, "title"), link, descrizione, data, immagine(voce));
    }

    /** Una voce senza titolo o senza un link web valido (ad esempio "javascript:...") viene scartata */
    private Optional<NotiziaResponse> creaNotizia(Fonte fonte, VoceGrezza voce) {
        String titolo = pulisciTesto(voce.titolo());
        String link = voce.link() == null ? "" : voce.link().trim();
        if (titolo.isEmpty() || !eUrlWeb(link)) {
            return Optional.empty();
        }
        String immagine = voce.immagine() == null ? null : voce.immagine().trim();
        if (immagine != null && !eUrlWeb(immagine)) {
            immagine = null;
        }
        return Optional.of(new NotiziaResponse(
                titolo,
                tronca(pulisciTesto(voce.descrizione())),
                link,
                fonte.nome(),
                fonte.lingua(),
                immagine,
                leggiData(voce.data())));
    }

    private String immagine(Element voce) {
        String miniatura = attributo(voce, "media:thumbnail", "url");
        if (miniatura != null) {
            return miniatura;
        }
        NodeList contenuti = voce.getElementsByTagName("media:content");
        for (int i = 0; i < contenuti.getLength(); i++) {
            Element contenuto = (Element) contenuti.item(i);
            boolean eImmagine = "image".equals(contenuto.getAttribute("medium"))
                    || contenuto.getAttribute("type").startsWith("image/");
            if (eImmagine && !contenuto.getAttribute("url").isBlank()) {
                return contenuto.getAttribute("url");
            }
        }
        NodeList allegati = voce.getElementsByTagName("enclosure");
        for (int i = 0; i < allegati.getLength(); i++) {
            Element allegato = (Element) allegati.item(i);
            if (allegato.getAttribute("type").startsWith("image/") && !allegato.getAttribute("url").isBlank()) {
                return allegato.getAttribute("url");
            }
        }
        return null;
    }

    private String testo(Element voce, String nomeTag) {
        NodeList elementi = voce.getElementsByTagName(nomeTag);
        return elementi.getLength() == 0 ? null : elementi.item(0).getTextContent();
    }

    private String attributo(Element voce, String nomeTag, String nomeAttributo) {
        NodeList elementi = voce.getElementsByTagName(nomeTag);
        if (elementi.getLength() == 0) {
            return null;
        }
        String valore = ((Element) elementi.item(0)).getAttribute(nomeAttributo);
        return valore.isBlank() ? null : valore;
    }

    private Instant leggiData(String testo) {
        if (testo == null || testo.isBlank()) {
            return null;
        }
        String data = testo.trim();
        try {
            return ZonedDateTime.parse(data, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(); // RSS
        } catch (DateTimeParseException ignorata) {
            // si prova il formato successivo
        }
        try {
            return OffsetDateTime.parse(data).toInstant(); // Atom
        } catch (DateTimeParseException ignorata) {
            return null;
        }
    }

    private boolean eUrlWeb(String indirizzo) {
        try {
            String schema = URI.create(indirizzo).getScheme();
            return "http".equalsIgnoreCase(schema) || "https".equalsIgnoreCase(schema);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Toglie i tag HTML e le entità, e normalizza gli spazi: il frontend mostra solo testo semplice */
    private String pulisciTesto(String html) {
        if (html == null) {
            return "";
        }
        String testo = html.replaceAll("<[^>]*>", " ");
        testo = HtmlUtils.htmlUnescape(testo);
        testo = testo.replaceAll("<[^>]*>", " "); // tag che erano scritti come entità
        return testo.replaceAll("\\s+", " ").replaceAll("\\s+([.,;:!?])", "$1").trim();
    }

    private String tronca(String testo) {
        if (testo.length() <= LUNGHEZZA_MASSIMA_SINTESI) {
            return testo;
        }
        return testo.substring(0, LUNGHEZZA_MASSIMA_SINTESI - 3).trim() + "...";
    }

    private record VoceGrezza(String titolo, String link, String descrizione, String data, String immagine) {
    }
}