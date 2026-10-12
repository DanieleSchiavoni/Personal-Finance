// AdminControllerTest.java
package org.elis.progettoesempio.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.elis.progettoesempio.dto.request.ImpostaAbilitazioneRequest;
import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.entity.enumerazioni.RuoloUtente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AdminControllerTest extends ControllerTestBase {

    private Utente admin;
    private Utente mario;

    @BeforeEach
    void preparaDati() {
        admin = creaUtente("admin_test", RuoloUtente.ADMIN);
        mario = creaUtente("mario");
    }

    @Test
    void unUtenteComuneNonAccedeAlleFunzioniAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/statistiche").header(AUTORIZZAZIONE, bearer(mario)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errore").value("ACCESSO_NEGATO"));
    }

    @Test
    void ilAdminVedeLeStatisticheGenerali() throws Exception {
        mockMvc.perform(get("/api/admin/statistiche").header(AUTORIZZAZIONE, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utentiTotali").value(2))
                .andExpect(jsonPath("$.amministratori").value(1));
    }

    @Test
    void ilAdminBloccaUnUtente() throws Exception {
        mockMvc.perform(patch("/api/admin/utenti/" + mario.getId() + "/abilitazione")
                .header(AUTORIZZAZIONE, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new ImpostaAbilitazioneRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abilitato").value(false));
    }
}