// controller/NotificaControllerTest.java
package org.elis.progettoesempio.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.elis.progettoesempio.entity.Notifica;
import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.repository.NotificaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotificaControllerTest extends ControllerTestBase {

    @Autowired
    private NotificaRepository notificaRepository;

    private Utente mario;
    private Utente luca;
    private Notifica notificaDiMario;

    @BeforeEach
    void preparaDati() {
        mario = creaUtente("mario");
        luca = creaUtente("luca");
        notificaDiMario = new Notifica();
        notificaDiMario.setUtente(mario);
        notificaDiMario.setTitolo("Budget quasi esaurito");
        notificaDiMario.setMessaggio("Il budget 'Ristoranti' ha raggiunto l'81,6%");
        notificaRepository.save(notificaDiMario);
    }

    @Test
    void leNotificheSonoPersonali() throws Exception {
        mockMvc.perform(get("/api/notifiche").header(AUTORIZZAZIONE, bearer(mario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titolo").value("Budget quasi esaurito"))
                .andExpect(jsonPath("$[0].letta").value(false));

        mockMvc.perform(get("/api/notifiche").header(AUTORIZZAZIONE, bearer(luca)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void segnareUnaNotificaComeLettaAggiornaIlConteggio() throws Exception {
        mockMvc.perform(get("/api/notifiche/conteggio-non-lette").header(AUTORIZZAZIONE, bearer(mario)))
                .andExpect(jsonPath("$.nonLette").value(1));

        mockMvc.perform(patch("/api/notifiche/" + notificaDiMario.getId() + "/letta")
                .header(AUTORIZZAZIONE, bearer(mario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.letta").value(true));

        mockMvc.perform(get("/api/notifiche/conteggio-non-lette").header(AUTORIZZAZIONE, bearer(mario)))
                .andExpect(jsonPath("$.nonLette").value(0));
    }

    @Test
    void unaNotificaAltruiNonSiLeggeNeSiElimina() throws Exception {
        mockMvc.perform(patch("/api/notifiche/" + notificaDiMario.getId() + "/letta")
                .header(AUTORIZZAZIONE, bearer(luca)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/notifiche/" + notificaDiMario.getId())
                .header(AUTORIZZAZIONE, bearer(luca)))
                .andExpect(status().isNotFound());
    }
}