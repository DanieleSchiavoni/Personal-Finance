// GestioneErroriTest.java
package org.elis.progettoesempio.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Verifica che anche gli errori "di sistema" abbiano la risposta standard */
class GestioneErroriTest extends ControllerTestBase {

    private SpazioLavoro spazio;
    private Utente mario;

    @BeforeEach
    void preparaDati() {
        spazio = creaSpazio(TipoSpazio.PERSONALE);
        mario = creaUtente("mario");
        aggiungiMembro(spazio, mario, RuoloSpazio.PROPRIETARIO);
    }

    @Test
    void unParametroNonNumericoRestituisce400() throws Exception {
        mockMvc.perform(get("/api/spazi-lavoro/" + spazio.getId() + "/dashboard/riepilogo")
                .header(AUTORIZZAZIONE, bearer(mario))
                .param("mese", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errore").value("RICHIESTA_NON_VALIDA"));
    }

    @Test
    void unMeseFuoriIntervalloRestituisce400() throws Exception {
        mockMvc.perform(get("/api/spazi-lavoro/" + spazio.getId() + "/dashboard/riepilogo")
                .header(AUTORIZZAZIONE, bearer(mario))
                .param("mese", "13")
                .param("anno", "2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errore").value("RICHIESTA_NON_VALIDA"));
    }

    @Test
    void unJsonMalformatoRestituisce400() throws Exception {
        mockMvc.perform(post("/api/spazi-lavoro")
                .header(AUTORIZZAZIONE, bearer(mario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errore").value("RICHIESTA_NON_VALIDA"));
    }

    @Test
    void unPercorsoInesistenteRestituisce404ConLaRispostaStandard() throws Exception {
        mockMvc.perform(get("/api/non-esiste").header(AUTORIZZAZIONE, bearer(mario)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errore").value("NON_TROVATO"))
                .andExpect(jsonPath("$.percorso").value("/api/non-esiste"));
    }
}