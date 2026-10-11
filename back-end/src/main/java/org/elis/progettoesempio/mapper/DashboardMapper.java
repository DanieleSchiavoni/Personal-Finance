package org.elis.progettoesempio.mapper;

import java.math.BigDecimal;

import org.elis.progettoesempio.dto.response.TotaleCategoriaResponse;
import org.elis.progettoesempio.repository.TotaleCategoria;
import org.springframework.stereotype.Component;

@Component
public class DashboardMapper {

    public TotaleCategoriaResponse aTotaleCategoriaResponse(TotaleCategoria totale, BigDecimal percentuale) {
        return new TotaleCategoriaResponse(
                totale.getCategoriaId(),
                totale.getCategoriaNome(),
                totale.getColore(),
                totale.getTotale(),
                percentuale);
    }
}