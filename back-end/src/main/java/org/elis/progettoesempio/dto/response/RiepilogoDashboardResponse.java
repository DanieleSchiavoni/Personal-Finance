// RiepilogoDashboardResponse.java
package org.elis.progettoesempio.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record RiepilogoDashboardResponse(
        int mese,
        int anno,
        BigDecimal saldoTotale,
        BigDecimal entrateMese,
        BigDecimal usciteMese,
        BigDecimal risparmioMese,
        long transazioniMese,
        SintesiBudget budget,
        SintesiObiettivi obiettivi,
        List<TotaleCategoriaResponse> categorieConPiuSpesa,
        List<TransazioneResponse> ultimeTransazioni) {

    /** speso = quanto speso nelle sole categorie che hanno un budget nel mese */
    public record SintesiBudget(BigDecimal totale, BigDecimal speso, BigDecimal percentuale) {
    }

    public record SintesiObiettivi(
            int totali,
            int completati,
            BigDecimal importoRisparmiato,
            BigDecimal importoObiettivi) {
    }
}