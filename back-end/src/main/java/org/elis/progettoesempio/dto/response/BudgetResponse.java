// BudgetResponse.java
package org.elis.progettoesempio.dto.response;

import java.math.BigDecimal;

import org.elis.progettoesempio.entity.enumerazioni.StatoBudget;

/** disponibile diventa negativo se il budget è stato superato */
public record BudgetResponse(
        Long id,
        Long categoriaId,
        String categoriaNome,
        String categoriaColore,
        int mese,
        int anno,
        BigDecimal importo,
        BigDecimal speso,
        BigDecimal disponibile,
        BigDecimal percentuale,
        StatoBudget stato) {
}