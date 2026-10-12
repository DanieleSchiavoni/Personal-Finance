// SpazioLavoroControllerTest.java
package org.elis.progettoesempio.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.elis.progettoesempio.dto.request.AggiungiMembroRequest;
import org.elis.progettoesempio.dto.request.CambiaRuoloMembroRequest;
import org.elis.progettoesempio.dto.request.CreaSpazioLavoroRequest;
import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SpazioLavoroControllerTest extends ControllerTestBase {

    private SpazioLavoro azienda;
    private Utente proprietario;
    private Utente membro;
    private Utente esterno;

    @BeforeEach
    void preparaDati() {
        azienda = creaSpazio(TipoSpazio.AZIENDALE);
        proprietario = creaUtente("proprietario");
        membro = creaUtente("membro");
        esterno = creaUtente("esterno");
        aggiungiMembro(azienda, proprietario, RuoloSpazio.PROPRIETARIO);
        aggiungiMembro(azienda, membro, RuoloSpazio.MEMBRO);
    }

    @Test
    void creareUnoSpazioAziendaleRestituisce201() throws Exception {
        mockMvc.perform(post("/api/spazi-lavoro")
                .header(AUTORIZZAZIONE, bearer(esterno))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new CreaSpazioLavoroRequest("Acme S.r.l.", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("AZIENDALE"))
                .andExpect(jsonPath("$.ruoloUtente").value("PROPRIETARIO"))
                .andExpect(jsonPath("$.valuta").value("EUR"));
    }

    @Test
    void ilProprietarioAggiungeUnMembroPerEmail() throws Exception {
        String percorso = "/api/spazi-lavoro/" + azienda.getId() + "/membri";
        String corpo = json(new AggiungiMembroRequest("esterno@test.it", RuoloSpazio.MEMBRO));

        mockMvc.perform(post(percorso).header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruolo").value("MEMBRO"));

        // Aggiungerlo di nuovo: conflitto
        mockMvc.perform(post(percorso).header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errore").value("CONFLITTO"));

        // Email di un utente non registrato: non trovato
        mockMvc.perform(post(percorso).header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new AggiungiMembroRequest("sconosciuto@test.it", RuoloSpazio.MEMBRO))))
                .andExpect(status().isNotFound());
    }

    @Test
    void soloIlProprietarioCambiaIRuoli() throws Exception {
        mockMvc.perform(patch("/api/spazi-lavoro/" + azienda.getId() + "/membri/" + proprietario.getId())
                .header(AUTORIZZAZIONE, bearer(membro))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new CambiaRuoloMembroRequest(RuoloSpazio.LETTORE))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errore").value("ACCESSO_NEGATO"));
    }

    @Test
    void unEsternoNonVedeLoSpazio() throws Exception {
        mockMvc.perform(get("/api/spazi-lavoro/" + azienda.getId()).header(AUTORIZZAZIONE, bearer(esterno)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errore").value("NON_TROVATO"));
    }
}