// controller/DashboardControllerTest.java
package org.elis.progettoesempio.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Transazione;
import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.entity.enumerazioni.MetodoPagamento;

import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardControllerTest extends ControllerTestBase {

    private SpazioLavoro spazio;
    private Utente proprietario;
    private Categoria vendite;
    private Categoria affitto;

    @BeforeEach
    void preparaDati() {
        spazio = creaSpazio(TipoSpazio.AZIENDALE);
        proprietario = creaUtente("proprietario");
        aggiungiMembro(spazio, proprietario, RuoloSpazio.PROPRIETARIO);
        vendite = creaCategoria(spazio, "Vendite", TipoTransazione.ENTRATA);
        affitto = creaCategoria(spazio, "Affitto", TipoTransazione.USCITA);
    }

    @Test
    void ilRiepilogoMostraSaldoEntrateEUsciteDelMese() throws Exception {
        salva(vendite, TipoTransazione.ENTRATA, "300.00", LocalDate.of(2026, 9, 15)); // mese precedente
        salva(vendite, TipoTransazione.ENTRATA, "2000.00", LocalDate.of(2026, 10, 3));
        salva(affitto, TipoTransazione.USCITA, "500.00", LocalDate.of(2026, 10, 5));

        mockMvc.perform(get(percorso() + "/riepilogo").header(AUTORIZZAZIONE, bearer(proprietario))
                .param("mese", "10").param("anno", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoTotale").value(1800.0))
                .andExpect(jsonPath("$.entrateMese").value(2000.0))
                .andExpect(jsonPath("$.usciteMese").value(500.0))
                .andExpect(jsonPath("$.risparmioMese").value(1500.0))
                .andExpect(jsonPath("$.transazioniMese").value(2))
                .andExpect(jsonPath("$.categorieConPiuSpesa[0].categoriaNome").value("Affitto"));
    }

    @Test
    void ilTrendHaUnPuntoPerOgniMeseAncheSeVuoto() throws Exception {
        salva(affitto, TipoTransazione.USCITA, "100.00", LocalDate.of(2026, 8, 20));
        salva(vendite, TipoTransazione.ENTRATA, "1000.00", LocalDate.of(2026, 10, 3));

        mockMvc.perform(get(percorso() + "/trend").header(AUTORIZZAZIONE, bearer(proprietario))
                .param("granularita", "MESE").param("dal", "2026-08-15").param("al", "2026-10-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].uscite").value(100.0))
                .andExpect(jsonPath("$[1].entrate").value(0.0))
                .andExpect(jsonPath("$[2].saldoProgressivo").value(900.0));
    }

    private String percorso() {
        return "/api/spazi-lavoro/" + spazio.getId() + "/dashboard";
    }

    private void salva(Categoria categoria, TipoTransazione tipo, String importo, LocalDate data) {
        Transazione transazione = new Transazione();
        transazione.setImporto(new BigDecimal(importo));
        transazione.setDescrizione("Movimento di prova");
        transazione.setDataTransazione(data);
        transazione.setTipo(tipo);
        transazione.setMetodoPagamento(MetodoPagamento.BONIFICO);
        transazione.setSpazioLavoro(spazio);
        transazione.setCategoria(categoria);
        transazioneRepository.save(transazione);
    }
}