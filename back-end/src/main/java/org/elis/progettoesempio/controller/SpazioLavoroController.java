// SpazioLavoroController.java
package org.elis.progettoesempio.controller;

import java.util.List;

import org.elis.progettoesempio.dto.request.*;
import org.elis.progettoesempio.dto.response.MembroResponse;
import org.elis.progettoesempio.dto.response.SpazioLavoroResponse;
import org.elis.progettoesempio.security.UtenteCorrente;
import org.elis.progettoesempio.service.SpazioLavoroService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/spazi-lavoro")
@RequiredArgsConstructor
public class SpazioLavoroController {

    private final SpazioLavoroService spazioLavoroService;

    @GetMapping
    public List<SpazioLavoroResponse> elenca(@UtenteCorrente Long utenteId) {
        return spazioLavoroService.elencaSpaziUtente(utenteId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpazioLavoroResponse crea(@UtenteCorrente Long utenteId,
            @Valid @RequestBody CreaSpazioLavoroRequest richiesta) {
        return spazioLavoroService.creaSpazioAziendale(utenteId, richiesta);
    }

    @GetMapping("/{spazioId}")
    public SpazioLavoroResponse dettaglio(@PathVariable Long spazioId, @UtenteCorrente Long utenteId) {
        return spazioLavoroService.dettaglio(spazioId, utenteId);
    }

    @PutMapping("/{spazioId}")
    public SpazioLavoroResponse aggiorna(@PathVariable Long spazioId, @UtenteCorrente Long utenteId,
            @Valid @RequestBody AggiornaSpazioLavoroRequest richiesta) {
        return spazioLavoroService.aggiorna(spazioId, utenteId, richiesta);
    }

    @DeleteMapping("/{spazioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void elimina(@PathVariable Long spazioId, @UtenteCorrente Long utenteId) {
        spazioLavoroService.elimina(spazioId, utenteId);
    }

    @GetMapping("/{spazioId}/membri")
    public List<MembroResponse> elencaMembri(@PathVariable Long spazioId, @UtenteCorrente Long utenteId) {
        return spazioLavoroService.elencaMembri(spazioId, utenteId);
    }

    @PostMapping("/{spazioId}/membri")
    @ResponseStatus(HttpStatus.CREATED)
    public MembroResponse aggiungiMembro(@PathVariable Long spazioId, @UtenteCorrente Long utenteId,
            @Valid @RequestBody AggiungiMembroRequest richiesta) {
        return spazioLavoroService.aggiungiMembro(spazioId, utenteId, richiesta);
    }

    @PatchMapping("/{spazioId}/membri/{membroUtenteId}")
    public MembroResponse cambiaRuoloMembro(@PathVariable Long spazioId, @PathVariable Long membroUtenteId,
            @UtenteCorrente Long utenteId, @Valid @RequestBody CambiaRuoloMembroRequest richiesta) {
        return spazioLavoroService.cambiaRuoloMembro(spazioId, utenteId, membroUtenteId, richiesta);
    }

    @DeleteMapping("/{spazioId}/membri/{membroUtenteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rimuoviMembro(@PathVariable Long spazioId, @PathVariable Long membroUtenteId,
            @UtenteCorrente Long utenteId) {
        spazioLavoroService.rimuoviMembro(spazioId, utenteId, membroUtenteId);
    }
}