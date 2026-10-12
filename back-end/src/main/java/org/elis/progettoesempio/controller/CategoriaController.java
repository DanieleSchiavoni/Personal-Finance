// CategoriaController.java
package org.elis.progettoesempio.controller;

import java.util.List;

import org.elis.progettoesempio.dto.request.CategoriaRequest;
import org.elis.progettoesempio.dto.response.CategoriaResponse;
import org.elis.progettoesempio.security.UtenteCorrente;
import org.elis.progettoesempio.service.CategoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/spazi-lavoro/{spazioId}/categorie")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    public List<CategoriaResponse> elenca(@PathVariable Long spazioId, @UtenteCorrente Long utenteId) {
        return categoriaService.elenca(spazioId, utenteId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse crea(@PathVariable Long spazioId, @UtenteCorrente Long utenteId,
            @Valid @RequestBody CategoriaRequest richiesta) {
        return categoriaService.crea(spazioId, utenteId, richiesta);
    }

    @PutMapping("/{categoriaId}")
    public CategoriaResponse aggiorna(@PathVariable Long spazioId, @PathVariable Long categoriaId,
            @UtenteCorrente Long utenteId, @Valid @RequestBody CategoriaRequest richiesta) {
        return categoriaService.aggiorna(spazioId, utenteId, categoriaId, richiesta);
    }

    @DeleteMapping("/{categoriaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void elimina(@PathVariable Long spazioId, @PathVariable Long categoriaId,
            @UtenteCorrente Long utenteId) {
        categoriaService.elimina(spazioId, utenteId, categoriaId);
    }
}