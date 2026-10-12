// AdminController.java
package org.elis.progettoesempio.controller;

import java.util.List;

import org.elis.progettoesempio.dto.request.CategoriaGlobaleRequest;
import org.elis.progettoesempio.dto.request.ImpostaAbilitazioneRequest;
import org.elis.progettoesempio.dto.request.RicercaUtentiRequest;
import org.elis.progettoesempio.dto.response.CategoriaGlobaleResponse;
import org.elis.progettoesempio.dto.response.PaginaResponse;
import org.elis.progettoesempio.dto.response.StatisticheGeneraliResponse;
import org.elis.progettoesempio.dto.response.UtenteResponse;
import org.elis.progettoesempio.security.UtenteCorrente;
import org.elis.progettoesempio.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Riservato agli amministratori: dalla Fase 6 anche protetto da Spring Security */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/utenti")
    public PaginaResponse<UtenteResponse> elencaUtenti(@UtenteCorrente Long adminId,
            @Valid @ModelAttribute RicercaUtentiRequest ricerca) {
        return adminService.elencaUtenti(adminId, ricerca);
    }

    @PatchMapping("/utenti/{utenteId}/abilitazione")
    public UtenteResponse impostaAbilitazione(@PathVariable Long utenteId, @UtenteCorrente Long adminId,
            @Valid @RequestBody ImpostaAbilitazioneRequest richiesta) {
        return adminService.impostaAbilitazione(adminId, utenteId, richiesta.abilitato());
    }

    @GetMapping("/categorie")
    public List<CategoriaGlobaleResponse> elencaCategorie(@UtenteCorrente Long adminId) {
        return adminService.elencaCategorieGlobali(adminId);
    }

    @PostMapping("/categorie")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaGlobaleResponse creaCategoria(@UtenteCorrente Long adminId,
            @Valid @RequestBody CategoriaGlobaleRequest richiesta) {
        return adminService.creaCategoriaGlobale(adminId, richiesta);
    }

    @PutMapping("/categorie/{categoriaId}")
    public CategoriaGlobaleResponse aggiornaCategoria(@PathVariable Long categoriaId,
            @UtenteCorrente Long adminId, @Valid @RequestBody CategoriaGlobaleRequest richiesta) {
        return adminService.aggiornaCategoriaGlobale(adminId, categoriaId, richiesta);
    }

    @DeleteMapping("/categorie/{categoriaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminaCategoria(@PathVariable Long categoriaId, @UtenteCorrente Long adminId) {
        adminService.eliminaCategoriaGlobale(adminId, categoriaId);
    }

    @GetMapping("/statistiche")
    public StatisticheGeneraliResponse statistiche(@UtenteCorrente Long adminId) {
        return adminService.statistiche(adminId);
    }
}