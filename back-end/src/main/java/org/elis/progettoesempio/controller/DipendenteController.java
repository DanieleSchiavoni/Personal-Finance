// DipendenteController.java
package org.elis.progettoesempio.controller;

import java.util.List;

import org.elis.progettoesempio.dto.request.DipendenteRequest;
import org.elis.progettoesempio.dto.request.PagamentoStipendioRequest;
import org.elis.progettoesempio.dto.response.DipendenteResponse;
import org.elis.progettoesempio.dto.response.PagamentoStipendioResponse;
import org.elis.progettoesempio.dto.response.TransazioneResponse;
import org.elis.progettoesempio.security.UtenteCorrente;
import org.elis.progettoesempio.service.DipendenteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/spazi-lavoro/{spazioId}/dipendenti")
@RequiredArgsConstructor
public class DipendenteController {

    private final DipendenteService dipendenteService;

    @GetMapping
    public List<DipendenteResponse> elenca(@PathVariable Long spazioId, @UtenteCorrente Long utenteId) {
        return dipendenteService.elenca(spazioId, utenteId);
    }

    @GetMapping("/{dipendenteId}")
    public DipendenteResponse dettaglio(@PathVariable Long spazioId, @PathVariable Long dipendenteId,
            @UtenteCorrente Long utenteId) {
        return dipendenteService.dettaglio(spazioId, utenteId, dipendenteId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DipendenteResponse crea(@PathVariable Long spazioId, @UtenteCorrente Long utenteId,
            @Valid @RequestBody DipendenteRequest richiesta) {
        return dipendenteService.crea(spazioId, utenteId, richiesta);
    }

    @PutMapping("/{dipendenteId}")
    public DipendenteResponse aggiorna(@PathVariable Long spazioId, @PathVariable Long dipendenteId,
            @UtenteCorrente Long utenteId, @Valid @RequestBody DipendenteRequest richiesta) {
        return dipendenteService.aggiorna(spazioId, utenteId, dipendenteId, richiesta);
    }

    @DeleteMapping("/{dipendenteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void elimina(@PathVariable Long spazioId, @PathVariable Long dipendenteId,
            @UtenteCorrente Long utenteId) {
        dipendenteService.elimina(spazioId, utenteId, dipendenteId);
    }

    /** Registra lo stipendio di un mese: restituisce la transazione di uscita creata */
    @PostMapping("/{dipendenteId}/pagamenti-stipendio")
    @ResponseStatus(HttpStatus.CREATED)
    public TransazioneResponse registraPagamento(@PathVariable Long spazioId, @PathVariable Long dipendenteId,
            @UtenteCorrente Long utenteId, @Valid @RequestBody PagamentoStipendioRequest richiesta) {
        return dipendenteService.registraPagamentoStipendio(spazioId, utenteId, dipendenteId, richiesta);
    }

    @GetMapping("/{dipendenteId}/pagamenti-stipendio")
    public List<PagamentoStipendioResponse> elencaPagamenti(@PathVariable Long spazioId,
            @PathVariable Long dipendenteId, @UtenteCorrente Long utenteId) {
        return dipendenteService.elencaPagamenti(spazioId, utenteId, dipendenteId);
    }
}