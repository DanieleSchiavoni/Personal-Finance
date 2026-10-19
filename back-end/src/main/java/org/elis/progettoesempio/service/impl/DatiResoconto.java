// service/impl/DatiResoconto.java
package org.elis.progettoesempio.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.elis.progettoesempio.dto.response.BudgetResponse;
import org.elis.progettoesempio.dto.response.TransazioneResponse;

/** Tutto ciò che serve a disegnare il PDF: raccolto dal service, disegnato dal generatore */
public record DatiResoconto(
        String nomeSpazio,
        String valuta,
        YearMonth periodo,
        LocalDateTime generatoIl,
        BigDecimal entrate,
        BigDecimal uscite,
        List<RigaCategoria> entratePerCategoria,
        List<RigaCategoria> uscitePerCategoria,
        List<BudgetResponse> budget,
        List<RigaStipendio> stipendi,
        List<TransazioneResponse> transazioni) {

    public record RigaCategoria(String nome, BigDecimal totale) {
    }

    public record RigaStipendio(String dipendente, BigDecimal importo, LocalDate data) {
    }

    public BigDecimal saldo() {
        return entrate.subtract(uscite);
    }

    public BigDecimal totaleStipendi() {
        return stipendi.stream().map(RigaStipendio::importo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}