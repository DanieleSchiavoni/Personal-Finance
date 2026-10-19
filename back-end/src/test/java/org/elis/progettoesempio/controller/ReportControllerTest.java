// src/test/java/org/elis/progettoesempio/controller/ReportControllerTest.java
package org.elis.progettoesempio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Utente;

import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class ReportControllerTest extends ControllerTestBase {

    private SpazioLavoro spazio;
    private Utente proprietario;
    private Utente membro;

    @BeforeEach
    void preparaDati() {
        spazio = creaSpazio(TipoSpazio.AZIENDALE);
        proprietario = creaUtente("proprietario");
        membro = creaUtente("membro");
        aggiungiMembro(spazio, proprietario, RuoloSpazio.PROPRIETARIO);
        aggiungiMembro(spazio, membro, RuoloSpazio.MEMBRO);
    }

    @Test
    void ilProprietarioScaricaIlPdfDelMese() throws Exception {
        MvcResult esito = mockMvc.perform(get("/api/spazi-lavoro/" + spazio.getId() + "/report/mensile")
                .header(AUTORIZZAZIONE, bearer(proprietario))
                .param("mese", "10")
                .param("anno", "2026"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_PDF_VALUE)))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("resoconto-2026-10.pdf")))
                .andReturn();

        byte[] corpo = esito.getResponse().getContentAsByteArray();
        assertThat(new String(corpo, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void unMembroRiceve403() throws Exception {
        mockMvc.perform(get("/api/spazi-lavoro/" + spazio.getId() + "/report/mensile")
                .header(AUTORIZZAZIONE, bearer(membro))
                .param("mese", "10")
                .param("anno", "2026"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errore").value("ACCESSO_NEGATO"));
    }
}