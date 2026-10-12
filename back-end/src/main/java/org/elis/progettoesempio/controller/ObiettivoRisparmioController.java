// ObiettivoRisparmioController.java
package org.elis.progettoesempio.controller;

import java.util.List;

import org.elis.progettoesempio.dto.request.ObiettivoRisparmioRequest;
import org.elis.progettoesempio.dto.request.VersamentoRequest;
import org.elis.progettoesempio.dto.response.ObiettivoRisparmioResponse;
import org.elis.progettoesempio.security.UtenteCorrente;
import org.elis.progettoesempio.service.ObiettivoRisparmioService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/spazi-lavoro/{spazioId}/obiettivi")
@RequiredArgsConstructor
public class ObiettivoRisparmioController {

    private final ObiettivoRisparmioService obiettivoService;

    @GetMapping
    public List<ObiettivoRisparmioResponse> elenca(@PathVariable Long spazioId, @UtenteCorrente Long utenteId) {
        return obiettivoService.elenca(spazioId, utenteId);
    }

    @GetMapping("/{obiettivoId}")
    public ObiettivoRisparmioResponse dettaglio(@PathVariable Long spazioId, @PathVariable Long obiettivoId,
            @UtenteCorrente Long utenteId) {
        return obiettivoService.dettaglio(spazioId, utenteId, obiettivoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ObiettivoRisparmioResponse crea(@PathVariable Long spazioId, @UtenteCorrente Long utenteId,
            @Valid @RequestBody ObiettivoRisparmioRequest richiesta) {
        return obiettivoService.crea(spazioId, utenteId, richiesta);
    }

    @PutMapping("/{obiettivoId}")
    public ObiettivoRisparmioResponse aggiorna(@PathVariable Long spazioId, @PathVariable Long obiettivoId,
            @UtenteCorrente Long utenteId, @Valid @RequestBody ObiettivoRisparmioRequest richiesta) {
        return obiettivoService.aggiorna(spazioId, utenteId, obiettivoId, richiesta);
    }

    @DeleteMapping("/{obiettivoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void elimina(@PathVariable Long spazioId, @PathVariable Long obiettivoId,
            @UtenteCorrente Long utenteId) {
        obiettivoService.elimina(spazioId, utenteId, obiettivoId);
    }

    /** Aggiunge denaro all'obiettivo e restituisce l'obiettivo aggiornato */
    @PatchMapping("/{obiettivoId}/versamenti")
    public ObiettivoRisparmioResponse aggiungiVersamento(@PathVariable Long spazioId,
            @PathVariable Long obiettivoId, @UtenteCorrente Long utenteId,
            @Valid @RequestBody VersamentoRequest richiesta) {
        return obiettivoService.aggiungiVersamento(spazioId, utenteId, obiettivoId, richiesta);
    }
}