// controller/SicurezzaEndpointTest.java
package org.elis.progettoesempio.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Utente;

import org.elis.progettoesempio.entity.enumerazioni.RuoloUtente;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SicurezzaEndpointTest extends ControllerTestBase {

    private SpazioLavoro spazio;
    private Utente esterno;

    @BeforeEach
    void preparaDati() {
        spazio = creaSpazio(TipoSpazio.AZIENDALE);
        Utente proprietario = creaUtente("proprietario");
        esterno = creaUtente("esterno");
        aggiungiMembro(spazio, proprietario, RuoloSpazio.PROPRIETARIO);
    }

    /** Ogni riga della lista è un test: per aggiungere un endpoint basta aggiungere il suo percorso */
    @ParameterizedTest(name = "senza token {0} risponde 401")
    @ValueSource(strings = {
            "/api/utenti/me",
            "/api/spazi-lavoro",
            "/api/spazi-lavoro/1/transazioni",
            "/api/spazi-lavoro/1/categorie",
            "/api/spazi-lavoro/1/budget",
            "/api/spazi-lavoro/1/obiettivi",
            "/api/spazi-lavoro/1/dipendenti",
            "/api/spazi-lavoro/1/dashboard/riepilogo",
            "/api/spazi-lavoro/1/report/mensile",
            "/api/notifiche",
            "/api/admin/statistiche" })
    void senzaTokenGliEndpointProtettiRispondono401(String percorso) throws Exception {
        mockMvc.perform(get(percorso))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errore").value("NON_AUTENTICATO"));
    }

    @ParameterizedTest(name = "un esterno non accede a {0}")
    @ValueSource(strings = {
            "/transazioni", "/categorie", "/budget", "/obiettivi", "/dipendenti", "/membri",
            "/dashboard/riepilogo", "/dashboard/trend", "/dashboard/spese-per-categoria", "/report/mensile" })
    void unEsternoNonAccedeAgliSpaziAltrui(String risorsa) throws Exception {
        // Non è membro dello spazio: la risposta è 404, che non rivela nemmeno se lo spazio esiste
        mockMvc.perform(get("/api/spazi-lavoro/" + spazio.getId() + risorsa)
                .header(AUTORIZZAZIONE, bearer(esterno)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errore").value("NON_TROVATO"));
    }

    @Test
    void ancheUnAmministratoreDellaPiattaformaNonVedeIDatiFinanziariDegliSpazi() throws Exception {
        Utente admin = creaUtente("admin", RuoloUtente.ADMIN);

        mockMvc.perform(get("/api/spazi-lavoro/" + spazio.getId() + "/transazioni")
                .header(AUTORIZZAZIONE, bearer(admin)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/spazi-lavoro/" + spazio.getId() + "/dashboard/riepilogo")
                .header(AUTORIZZAZIONE, bearer(admin)))
                .andExpect(status().isNotFound());
    }
}