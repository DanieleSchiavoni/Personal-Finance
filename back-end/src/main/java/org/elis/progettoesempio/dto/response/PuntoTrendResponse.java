// PuntoTrendResponse.java
package org.elis.progettoesempio.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un periodo (settimana o mese) del grafico di andamento.
 * saldoPeriodo = entrate - uscite del periodo; saldoProgressivo = saldo complessivo a fine periodo.
 */
public record PuntoTrendResponse(
        String etichetta,
        LocalDate inizio,
        LocalDate fine,
        BigDecimal entrate,
        BigDecimal uscite,
        BigDecimal saldoPeriodo,
        BigDecimal saldoProgressivo) {
}