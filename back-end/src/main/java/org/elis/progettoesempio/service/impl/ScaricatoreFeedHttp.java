// service/impl/ScaricatoreFeedHttp.java
package org.elis.progettoesempio.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.elis.progettoesempio.service.ScaricatoreFeed;
import org.springframework.stereotype.Component;

@Component
public class ScaricatoreFeedHttp implements ScaricatoreFeed {

    private static final int DIMENSIONE_MASSIMA = 5 * 1024 * 1024;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public byte[] scarica(String url) throws IOException, InterruptedException {
        HttpRequest richiesta = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "PersonalFinance/1.0 (progetto portfolio; lettore RSS)")
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
                .GET()
                .build();

        HttpResponse<InputStream> risposta = client.send(richiesta, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream corpo = risposta.body()) {
            if (risposta.statusCode() != 200) {
                throw new IOException("Risposta HTTP " + risposta.statusCode() + " da " + url);
            }
            // Si legge al massimo un byte oltre il limite: basta per capire che il feed è troppo grande
            byte[] contenuto = corpo.readNBytes(DIMENSIONE_MASSIMA + 1);
            if (contenuto.length > DIMENSIONE_MASSIMA) {
                throw new IOException("Feed troppo grande: " + url);
            }
            return contenuto;
        }
    }
}