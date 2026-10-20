// controller/ObiettivoControllerTest.java
package org.elis.progettoesempio.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Utente;

import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class ObiettivoControllerTest extends ControllerTestBase {

    private SpazioLavoro spazio;
    private Utente proprietario;

    @BeforeEach
    void preparaDati() {
        spazio = creaSpazio(TipoSpazio.AZIENDALE);
        proprietario = creaUtente("proprietario");
        aggiungiMembro(spazio, proprietario, RuoloSpazio.PROPRIETARIO);
    }

    @Test
    void creareUnObiettivoRestituisce201() throws Exception {
        mockMvc.perform(post(percorso()).header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON).content(corpoObiettivo()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Nuova automobile"))
                .andExpect(jsonPath("$.stato").value("IN_CORSO"))
                .andExpect(jsonPath("$.percentuale").value(0.0));
    }

    @Test
    void unVersamentoAggiornaPercentualeEImportoMancante() throws Exception {
        MvcResult creato = mockMvc.perform(post(percorso()).header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON).content(corpoObiettivo()))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(creato.getResponse().getContentAsString()).get("id").asLong();

        // L'esempio della specifica: 7.500 su 20.000 = 37,5%
        mockMvc.perform(patch(percorso() + "/" + id + "/versamenti").header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON).content("{\"importo\":7500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importoAttuale").value(7500.0))
                .andExpect(jsonPath("$.importoMancante").value(12500.0))
                .andExpect(jsonPath("$.percentuale").value(37.5))
                .andExpect(jsonPath("$.stato").value("IN_CORSO"));
    }

    private String percorso() {
        return "/api/spazi-lavoro/" + spazio.getId() + "/obiettivi";
    }

    private String corpoObiettivo() {
        return "{\"nome\":\"Nuova automobile\",\"descrizione\":null,\"importoObiettivo\":20000,\"scadenza\":\""
                + LocalDate.now().plusYears(1) + "\"}";
    }
}