// BudgetController.java
package org.elis.progettoesempio.controller;

import java.time.YearMonth;
import java.util.List;

import org.elis.progettoesempio.dto.request.AggiornaBudgetRequest;
import org.elis.progettoesempio.dto.request.BudgetRequest;
import org.elis.progettoesempio.dto.response.BudgetResponse;
import org.elis.progettoesempio.security.UtenteCorrente;
import org.elis.progettoesempio.service.BudgetService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/spazi-lavoro/{spazioId}/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    /** Se mese e anno mancano si usa il mese corrente */
    @GetMapping
    public List<BudgetResponse> elenca(@PathVariable Long spazioId, @UtenteCorrente Long utenteId,
            @RequestParam(required = false) Integer mese, @RequestParam(required = false) Integer anno) {
        YearMonth oggi = YearMonth.now();
        return budgetService.elenca(spazioId, utenteId,
                mese != null ? mese : oggi.getMonthValue(),
                anno != null ? anno : oggi.getYear());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse crea(@PathVariable Long spazioId, @UtenteCorrente Long utenteId,
            @Valid @RequestBody BudgetRequest richiesta) {
        return budgetService.crea(spazioId, utenteId, richiesta);
    }

    /** Di un budget si modifica solo l'importo */
    @PatchMapping("/{budgetId}")
    public BudgetResponse aggiorna(@PathVariable Long spazioId, @PathVariable Long budgetId,
            @UtenteCorrente Long utenteId, @Valid @RequestBody AggiornaBudgetRequest richiesta) {
        return budgetService.aggiorna(spazioId, utenteId, budgetId, richiesta);
    }

    @DeleteMapping("/{budgetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void elimina(@PathVariable Long spazioId, @PathVariable Long budgetId, @UtenteCorrente Long utenteId) {
        budgetService.elimina(spazioId, utenteId, budgetId);
    }
}