// controller/NotizieControllerTest.java
package org.elis.progettoesempio.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class NotizieControllerTest extends ControllerTestBase {

    @Test
    void leNotizieSonoPubblicheESenzaTokenLaRispostaE200() throws Exception {
        // Nei test le fonti sono disabilitate: l'elenco è vuoto, ma l'endpoint non richiede l'accesso
        mockMvc.perform(get("/api/notizie").param("limite", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}