// TransazioneController.java
package org.elis.progettoesempio.controller;

import org.elis.progettoesempio.dto.request.RicercaTransazioniRequest;
import org.elis.progettoesempio.dto.request.TransazioneRequest;
import org.elis.progettoesempio.dto.response.PaginaResponse;
import org.elis.progettoesempio.dto.response.TransazioneResponse;
import org.elis.progettoesempio.security.UtenteCorrente;
import org.elis.progettoesempio.service.TransazioneService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/spazi-lavoro/{spazioId}/transazioni")
@RequiredArgsConstructor
public class TransazioneController {

    private final TransazioneService transazioneService;

    /** Filtri, paginazione e ordinamento arrivano come parametri: ?tipo=USCITA&dal=2026-10-01&pagina=0 */
    @GetMapping
    public PaginaResponse<TransazioneResponse> cerca(@PathVariable Long spazioId, @UtenteCorrente Long utenteId,
            @Valid @ModelAttribute RicercaTransazioniRequest ricerca) {
        return transazioneService.cerca(spazioId, utenteId, ricerca);
    }

    @GetMapping("/{transazioneId}")
    public TransazioneResponse dettaglio(@PathVariable Long spazioId, @PathVariable Long transazioneId,
            @UtenteCorrente Long utenteId) {
        return transazioneService.dettaglio(spazioId, utenteId, transazioneId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransazioneResponse crea(@PathVariable Long spazioId, @UtenteCorrente Long utenteId,
            @Valid @RequestBody TransazioneRequest richiesta) {
        return transazioneService.crea(spazioId, utenteId, richiesta);
    }

    @PutMapping("/{transazioneId}")
    public TransazioneResponse aggiorna(@PathVariable Long spazioId, @PathVariable Long transazioneId,
            @UtenteCorrente Long utenteId, @Valid @RequestBody TransazioneRequest richiesta) {
        return transazioneService.aggiorna(spazioId, utenteId, transazioneId, richiesta);
    }

    @DeleteMapping("/{transazioneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void elimina(@PathVariable Long spazioId, @PathVariable Long transazioneId,
            @UtenteCorrente Long utenteId) {
        transazioneService.elimina(spazioId, utenteId, transazioneId);
    }
}