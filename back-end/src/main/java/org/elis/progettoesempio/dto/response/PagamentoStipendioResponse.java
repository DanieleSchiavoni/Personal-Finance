// PagamentoStipendioResponse.java
package org.elis.progettoesempio.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagamentoStipendioResponse(
        Long transazioneId,
        int mese,
        int anno,
        BigDecimal importo,
        LocalDate dataPagamento) {
}