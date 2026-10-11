// service/BudgetService.java
package org.elis.progettoesempio.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.elis.progettoesempio.dto.request.AggiornaBudgetRequest;
import org.elis.progettoesempio.dto.request.BudgetRequest;
import org.elis.progettoesempio.dto.response.BudgetResponse;

public interface BudgetService {

    /** Budget di un mese, con speso, disponibile, percentuale e stato già calcolati */
    List<BudgetResponse> elenca(Long spazioLavoroId, Long utenteId, int mese, int anno);

    BudgetResponse crea(Long spazioLavoroId, Long utenteId, BudgetRequest richiesta);

    BudgetResponse aggiorna(Long spazioLavoroId, Long utenteId, Long budgetId, AggiornaBudgetRequest richiesta);

    void elimina(Long spazioLavoroId, Long utenteId, Long budgetId);

    /**
     * Chiamato dopo una spesa: se ha fatto scattare la soglia dell'80% o del 100%
     * crea una notifica per chi gestisce lo spazio.
     *
     * @param variazione quanto la spesa della categoria è aumentata con questa operazione
     */
    void controllaSoglie(Long spazioLavoroId, Long categoriaId, LocalDate dataSpesa, BigDecimal variazione);
}