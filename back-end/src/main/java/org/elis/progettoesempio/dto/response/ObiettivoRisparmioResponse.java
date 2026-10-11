// ObiettivoRisparmioResponse.java
package org.elis.progettoesempio.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.elis.progettoesempio.entity.enumerazioni.StatoObiettivo;

public record ObiettivoRisparmioResponse(
        Long id,
        String nome,
        String descrizione,
        BigDecimal importoObiettivo,
        BigDecimal importoAttuale,
        BigDecimal importoMancante,
        BigDecimal percentuale,
        LocalDate scadenza,
        StatoObiettivo stato) {
}