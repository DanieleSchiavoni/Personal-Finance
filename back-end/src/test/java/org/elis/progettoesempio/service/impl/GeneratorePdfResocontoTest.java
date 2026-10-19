// src/test/java/org/elis/progettoesempio/service/impl/GeneratorePdfResocontoTest.java
package org.elis.progettoesempio.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.elis.progettoesempio.dto.response.BudgetResponse;
import org.elis.progettoesempio.dto.response.TransazioneResponse;
import org.elis.progettoesempio.entity.enumerazioni.MetodoPagamento;
import org.elis.progettoesempio.entity.enumerazioni.StatoBudget;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.service.impl.DatiResoconto.RigaCategoria;
import org.elis.progettoesempio.service.impl.DatiResoconto.RigaStipendio;
import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;

class GeneratorePdfResocontoTest {

    private final GeneratorePdfResoconto generatore = new GeneratorePdfResoconto();

    @Test
    void generaUnPdfValidoConTutteLeSezioni() throws Exception {
        byte[] pdf = generatore.genera(datiCompleti(5, "Spesa supermercato"));

        assertThat(intestazione(pdf)).isEqualTo("%PDF-");
        assertThat(pagine(pdf)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void conMoltiMovimentiIlResocontoOccupaPiuPagine() throws Exception {
        byte[] pdf = generatore.genera(datiCompleti(150, "Movimento di prova"));

        assertThat(pagine(pdf)).isGreaterThan(1);
    }

    @Test
    void unMeseSenzaMovimentiGeneraComunqueUnPdfValido() throws Exception {
        DatiResoconto vuoto = new DatiResoconto("Spazio vuoto", "EUR", YearMonth.of(2026, 10),
                LocalDateTime.of(2026, 10, 15, 9, 30), BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(), List.of(), List.of(), List.of(), List.of());

        byte[] pdf = generatore.genera(vuoto);

        assertThat(intestazione(pdf)).isEqualTo("%PDF-");
        assertThat(pagine(pdf)).isEqualTo(1);
    }

    @Test
    void accentiESimboliSpecialiNonCreanoProblemi() throws Exception {
        byte[] pdf = generatore.genera(datiCompleti(3, "Caffè già così, è 5 € ☕ <b>test</b>"));

        assertThat(intestazione(pdf)).isEqualTo("%PDF-");
    }

    private String intestazione(byte[] pdf) {
        return new String(pdf, 0, 5, StandardCharsets.US_ASCII);
    }

    private int pagine(byte[] pdf) throws Exception {
        PdfReader lettore = new PdfReader(pdf);
        try {
            return lettore.getNumberOfPages();
        } finally {
            lettore.close();
        }
    }

    private DatiResoconto datiCompleti(int numeroTransazioni, String descrizione) {
        List<TransazioneResponse> transazioni = new ArrayList<>();
        for (int i = 0; i < numeroTransazioni; i++) {
            boolean uscita = i % 4 != 0;
            transazioni.add(new TransazioneResponse((long) i, new BigDecimal("12.50").add(BigDecimal.valueOf(i)),
                    descrizione + " " + i, LocalDate.of(2026, 10, 1 + (i % 28)),
                    uscita ? TipoTransazione.USCITA : TipoTransazione.ENTRATA, MetodoPagamento.CARTA, null,
                    1L, uscita ? "Alimentari" : "Stipendio", "#198754", 1L, "mario"));
        }
        BudgetResponse budget = new BudgetResponse(1L, 1L, "Alimentari", "#dc3545", 10, 2026,
                new BigDecimal("300.00"), new BigDecimal("245.00"), new BigDecimal("55.00"),
                new BigDecimal("81.6"), StatoBudget.ATTENZIONE);

        return new DatiResoconto("Azienda di prova", "EUR", YearMonth.of(2026, 10),
                LocalDateTime.of(2026, 10, 15, 9, 30),
                new BigDecimal("2000.00"), new BigDecimal("1245.00"),
                List.of(new RigaCategoria("Stipendio", new BigDecimal("2000.00"))),
                List.of(new RigaCategoria("Alimentari", new BigDecimal("245.00")),
                        new RigaCategoria("Casa", new BigDecimal("1000.00"))),
                List.of(budget),
                List.of(new RigaStipendio("Mario Rossi", new BigDecimal("1500.00"), LocalDate.of(2026, 10, 5))),
                transazioni);
    }
}