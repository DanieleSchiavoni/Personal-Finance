// NotificaController.java
package org.elis.progettoesempio.controller;

import java.util.List;

import org.elis.progettoesempio.dto.response.ConteggioNotificheResponse;
import org.elis.progettoesempio.dto.response.NotificaResponse;
import org.elis.progettoesempio.security.UtenteCorrente;
import org.elis.progettoesempio.service.NotificaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

/** Le notifiche sono personali: non dipendono dallo spazio di lavoro */
@RestController
@RequestMapping("/api/notifiche")
@RequiredArgsConstructor
public class NotificaController {

    private final NotificaService notificaService;

    @GetMapping
    public List<NotificaResponse> elenca(@UtenteCorrente Long utenteId) {
        return notificaService.elenca(utenteId);
    }

    @GetMapping("/conteggio-non-lette")
    public ConteggioNotificheResponse contaNonLette(@UtenteCorrente Long utenteId) {
        return new ConteggioNotificheResponse(notificaService.contaNonLette(utenteId));
    }

    @PatchMapping("/{notificaId}/letta")
    public NotificaResponse segnaComeLetta(@PathVariable Long notificaId, @UtenteCorrente Long utenteId) {
        return notificaService.segnaComeLetta(utenteId, notificaId);
    }

    @PatchMapping("/segna-tutte-come-lette")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void segnaTutteComeLette(@UtenteCorrente Long utenteId) {
        notificaService.segnaTutteComeLette(utenteId);
    }

    @DeleteMapping("/{notificaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void elimina(@PathVariable Long notificaId, @UtenteCorrente Long utenteId) {
        notificaService.elimina(utenteId, notificaId);
    }
}