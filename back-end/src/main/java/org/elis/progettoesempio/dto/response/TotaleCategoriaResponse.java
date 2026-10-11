// TotaleCategoriaResponse.java
package org.elis.progettoesempio.dto.response;

import java.math.BigDecimal;

/** percentuale = quota della categoria sul totale delle uscite del periodo */
public record TotaleCategoriaResponse(
        Long categoriaId,
        String categoriaNome,
        String colore,
        BigDecimal totale,
        BigDecimal percentuale) {
}