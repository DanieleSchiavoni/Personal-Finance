package org.elis.progettoesempio.controller;

import org.elis.progettoesempio.dto.request.AggiornaProfiloRequest;
import org.elis.progettoesempio.dto.request.CambiaPasswordRequest;
import org.elis.progettoesempio.dto.request.ConfermaPasswordRequest;
import org.elis.progettoesempio.dto.response.UtenteResponse;
import org.elis.progettoesempio.security.UtenteCorrente;
import org.elis.progettoesempio.service.AutenticazioneService;
import org.elis.progettoesempio.service.UtenteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/utenti")
@RequiredArgsConstructor
public class UtenteController {

    private final UtenteService utenteService;
    private final AutenticazioneService autenticazioneService;

    @GetMapping("/me")
    public UtenteResponse profilo(@UtenteCorrente Long utenteId) {
        return utenteService.profilo(utenteId);
    }

    @PutMapping("/me")
    public UtenteResponse aggiornaProfilo(@UtenteCorrente Long utenteId,
            @Valid @RequestBody AggiornaProfiloRequest richiesta) {
        return utenteService.aggiornaProfilo(utenteId, richiesta);
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiaPassword(@UtenteCorrente Long utenteId, @Valid @RequestBody CambiaPasswordRequest richiesta) {
        autenticazioneService.cambiaPassword(utenteId, richiesta);
    }

    /** Elimina l'account: serve la password attuale come conferma */
    @PostMapping("/me/eliminazione")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminaAccount(@UtenteCorrente Long utenteId, @Valid @RequestBody ConfermaPasswordRequest richiesta) {
        autenticazioneService.eliminaAccount(utenteId, richiesta);
    }
}