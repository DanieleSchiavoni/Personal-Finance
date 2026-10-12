package org.elis.progettoesempio.controller;

import org.elis.progettoesempio.dto.request.LoginRequest;
import org.elis.progettoesempio.dto.request.RegistrazioneRequest;
import org.elis.progettoesempio.dto.request.RinnovaTokenRequest;
import org.elis.progettoesempio.dto.response.AutenticazioneResponse;
import org.elis.progettoesempio.service.AutenticazioneService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AutenticazioneController {

    private final AutenticazioneService autenticazioneService;

    @PostMapping("/registrazione")
    @ResponseStatus(HttpStatus.CREATED)
    public AutenticazioneResponse registra(@Valid @RequestBody RegistrazioneRequest richiesta) {
        return autenticazioneService.registra(richiesta);
    }

    @PostMapping("/login")
    public AutenticazioneResponse login(@Valid @RequestBody LoginRequest richiesta) {
        return autenticazioneService.accedi(richiesta);
    }

    @PostMapping("/refresh")
    public AutenticazioneResponse refresh(@Valid @RequestBody RinnovaTokenRequest richiesta) {
        return autenticazioneService.rinnova(richiesta);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RinnovaTokenRequest richiesta) {
        autenticazioneService.esci(richiesta);
    }
}