// BudgetMapper.java
package org.elis.progettoesempio.mapper;

import java.math.BigDecimal;

import org.elis.progettoesempio.dto.response.BudgetResponse;
import org.elis.progettoesempio.entity.Budget;
import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.enumerazioni.StatoBudget;
import org.springframework.stereotype.Component;

@Component
public class BudgetMapper {

    /** I valori calcolati (speso, disponibile, percentuale, stato) li produce il service */
    public BudgetResponse aResponse(Budget budget, BigDecimal speso, BigDecimal disponibile,
            BigDecimal percentuale, StatoBudget stato) {
        Categoria categoria = budget.getCategoria();
        return new BudgetResponse(
                budget.getId(),
                categoria.getId(),
                categoria.getNome(),
                categoria.getColore(),
                budget.getMese(),
                budget.getAnno(),
                budget.getImporto(),
                speso,
                disponibile,
                percentuale,
                stato);
    }
}