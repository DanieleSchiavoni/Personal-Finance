// controller/BudgetControllerTest.java
package org.elis.progettoesempio.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;

class BudgetControllerTest extends ControllerTestBase {

    private SpazioLavoro spazio;
    private Utente proprietario;
    private Categoria ristoranti;

    @BeforeEach
    void preparaDati() {
        spazio = creaSpazio(TipoSpazio.AZIENDALE);
        proprietario = creaUtente("proprietario");
        aggiungiMembro(spazio, proprietario, RuoloSpazio.PROPRIETARIO);
        ristoranti = creaCategoria(spazio, "Ristoranti", TipoTransazione.USCITA);
    }

    @Test
    void creareUnBudgetRestituisce201ConIValoriCalcolati() throws Exception {
        mockMvc.perform(post(percorso()).header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON).content(corpo("300")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoriaNome").value("Ristoranti"))
                .andExpect(jsonPath("$.importo").value(300.0))
                .andExpect(jsonPath("$.speso").value(0.0))
                .andExpect(jsonPath("$.stato").value("OK"));
    }

    @Test
    void unBudgetDuplicatoNelloStessoMeseRestituisce409() throws Exception {
        mockMvc.perform(post(percorso()).header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON).content(corpo("300")))
                .andExpect(status().isCreated());

        mockMvc.perform(post(percorso()).header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON).content(corpo("500")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errore").value("CONFLITTO"));
    }

    @Test
    void unImportoNonValidoRestituisce400() throws Exception {
        mockMvc.perform(post(percorso()).header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON).content(corpo("0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errore").value("ERRORE_VALIDAZIONE"))
                .andExpect(jsonPath("$.campi[?(@.campo == 'importo')]").exists());
    }

    @Test
    void iBudgetMostranoSpesoPercentualeEStato() throws Exception {
        mockMvc.perform(post(percorso()).header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON).content(corpo("300")))
                .andExpect(status().isCreated());

        Transazione spesa = new Transazione();
        spesa.setImporto(new BigDecimal("245.00"));
        spesa.setDescrizione("Cene del mese");
        spesa.setDataTransazione(LocalDate.of(2026, 10, 5));
        spesa.setTipo(TipoTransazione.USCITA);
        spesa.setMetodoPagamento(MetodoPagamento.CARTA);
        spesa.setSpazioLavoro(spazio);
        spesa.setCategoria(ristoranti);
        transazioneRepository.save(spesa);

        // L'esempio della specifica: 245 su 300 = 81,6%, quindi in attenzione
        mockMvc.perform(get(percorso()).header(AUTORIZZAZIONE, bearer(proprietario))
                .param("mese", "10").param("anno", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].speso").value(245.0))
                .andExpect(jsonPath("$[0].disponibile").value(55.0))
                .andExpect(jsonPath("$[0].percentuale").value(81.6))
                .andExpect(jsonPath("$[0].stato").value("ATTENZIONE"));
    }

    private String percorso() {
        return "/api/spazi-lavoro/" + spazio.getId() + "/budget";
    }

    private String corpo(String importo) {
        return "{\"categoriaId\":" + ristoranti.getId() + ",\"importo\":" + importo + ",\"mese\":10,\"anno\":2026}";
    }
}