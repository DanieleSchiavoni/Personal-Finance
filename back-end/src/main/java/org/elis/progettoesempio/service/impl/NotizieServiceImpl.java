// service/impl/NotizieServiceImpl.java
package org.elis.progettoesempio.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.elis.progettoesempio.config.NotizieProprieta;
import org.elis.progettoesempio.config.NotizieProprieta.Fonte;
import org.elis.progettoesempio.dto.response.NotiziaResponse;
import org.elis.progettoesempio.service.NotizieService;
import org.elis.progettoesempio.service.ScaricatoreFeed;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotizieServiceImpl implements NotizieService {

    private static final int LIMITE_MASSIMO = 100;
    private static final int NOTIZIE_PER_FONTE = 15;
    /** Se nessuna fonte risponde si riprova presto, senza aspettare tutta la durata della cache */
    private static final Duration ATTESA_DOPO_ERRORE = Duration.ofMinutes(1);

    private final NotizieProprieta proprieta;
    private final ScaricatoreFeed scaricatore;
    private final LettoreFeedRss lettore;
    private final Clock orologio;

    // Un thread virtuale per ogni fonte: i download partono insieme
    private final ExecutorService esecutore = Executors.newVirtualThreadPerTaskExecutor();

    private List<NotiziaResponse> cache = List.of();
    private Instant scadenzaCache = Instant.MIN;

    public NotizieServiceImpl(NotizieProprieta proprieta, ScaricatoreFeed scaricatore, LettoreFeedRss lettore,
            Clock orologio) {
        this.proprieta = proprieta;
        this.scaricatore = scaricatore;
        this.lettore = lettore;
        this.orologio = orologio;
    }

    @Override
    public List<NotiziaResponse> ultime(int limite) {
        int quante = Math.max(1, Math.min(limite, LIMITE_MASSIMO));
        return notizieAggiornate().stream().limit(quante).toList();
    }

    /** Se la cache è scaduta si riscaricano i feed; altrimenti si usa quella che c'è */
    private synchronized List<NotiziaResponse> notizieAggiornate() {
        if (!proprieta.abilitato()) {
            return List.of();
        }
        Instant adesso = orologio.instant();
        if (adesso.isBefore(scadenzaCache)) {
            return cache;
        }

        List<NotiziaResponse> nuove = scaricaTutte();
        if (!nuove.isEmpty()) {
            cache = nuove; // se tutte le fonti falliscono, si continua a servire l'ultima cache valida
        }
        Duration durata = nuove.isEmpty() ? ATTESA_DOPO_ERRORE : Duration.ofMinutes(proprieta.minutiCache());
        scadenzaCache = adesso.plus(durata);
        return cache;
    }

    private List<NotiziaResponse> scaricaTutte() {
        List<CompletableFuture<List<NotiziaResponse>>> operazioni = proprieta.fonti().stream()
                .map(fonte -> CompletableFuture.supplyAsync(() -> leggiFonte(fonte), esecutore))
                .toList();

        Set<String> giaViste = new HashSet<>();
        return operazioni.stream()
                .flatMap(operazione -> operazione.join().stream())
                // Dalla più recente; senza data in fondo
                .sorted(Comparator.comparing(NotiziaResponse::pubblicataIl,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                // Lo stesso articolo può comparire in più feed: si tiene una volta sola
                .filter(notizia -> giaViste.add(notizia.url()))
                .toList();
    }

    /** Una fonte che non risponde non deve bloccare le altre: l'errore si registra e si prosegue */
    private List<NotiziaResponse> leggiFonte(Fonte fonte) {
        try {
            byte[] contenuto = scaricatore.scarica(fonte.url());
            return lettore.leggi(fonte, contenuto).stream().limit(NOTIZIE_PER_FONTE).toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (Exception e) {
            log.warn("Fonte di notizie non disponibile ({}): {}", fonte.nome(), e.getMessage());
            return List.of();
        }
    }

    @PreDestroy
    void chiudi() {
        esecutore.close();
    }
}