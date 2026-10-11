// DipendenteResponse.java
package org.elis.progettoesempio.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DipendenteResponse(
        Long id,
        String nome,
        String cognome,
        String mansione,
        BigDecimal stipendioMensile,
        LocalDate dataAssunzione,
        boolean attivo) {
}