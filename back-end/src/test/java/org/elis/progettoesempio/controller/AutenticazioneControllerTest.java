package org.elis.progettoesempio.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.elis.progettoesempio.dto.request.CambiaPasswordRequest;
import org.elis.progettoesempio.dto.request.ConfermaPasswordRequest;
import org.elis.progettoesempio.dto.request.LoginRequest;
import org.elis.progettoesempio.dto.request.RegistrazioneRequest;
import org.elis.progettoesempio.dto.request.RinnovaTokenRequest;
import org.elis.progettoesempio.entity.Utente;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class AutenticazioneControllerTest extends ControllerTestBase {

    private static final String PASSWORD = "Password123";

    @Test
    void laRegistrazioneCreaUtenteESpazioPersonale() throws Exception {
        MvcResult esito = mockMvc.perform(post("/api/auth/registrazione")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(registrazione("mario", "mario@test.it", PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.utente.username").value("mario"))
                .andExpect(jsonPath("$.utente.ruolo").value("UTENTE"))
                .andReturn();

        // Il token appena ricevuto apre le API protette e lo spazio personale esiste già
        mockMvc.perform(get("/api/spazi-lavoro")
                .header(AUTORIZZAZIONE, "Bearer " + campo(esito, "accessToken")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("PERSONALE"));
    }

    @Test
    void unaPasswordDeboleViene_rifiutata() throws Exception {
        mockMvc.perform(post("/api/auth/registrazione")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(registrazione("mario", "mario@test.it", "corta"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errore").value("ERRORE_VALIDAZIONE"))
                .andExpect(jsonPath("$.campi[?(@.campo == 'password')]").exists());
    }

    @Test
    void usernameOEmailGiaUsatiRestituisconoConflitto() throws Exception {
        registra("mario", "mario@test.it");

        mockMvc.perform(post("/api/auth/registrazione")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(registrazione("mario", "altra@test.it", PASSWORD))))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/registrazione")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(registrazione("altro", "mario@test.it", PASSWORD))))
                .andExpect(status().isConflict());
    }

    @Test
    void ilLoginFunzionaConUsernameEConEmail() throws Exception {
        registra("mario", "mario@test.it");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(new LoginRequest("mario", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(new LoginRequest("MARIO@test.it", PASSWORD))))
                .andExpect(status().isOk());
    }

    @Test
    void conCredenzialiSbagliateLaRispostaE401() throws Exception {
        registra("mario", "mario@test.it");

        // Password sbagliata e utente inesistente: stessa risposta
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(new LoginRequest("mario", "Sbagliata1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errore").value("NON_AUTENTICATO"))
                .andExpect(jsonPath("$.messaggio").value("Credenziali non valide"));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(new LoginRequest("inesistente", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.messaggio").value("Credenziali non valide"));
    }

    @Test
    void ilRefreshTokenSiUsaUnaVoltaSola() throws Exception {
        String primo = campo(registra("mario", "mario@test.it"), "refreshToken");

        MvcResult rinnovo = mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json(new RinnovaTokenRequest(primo))))
                .andExpect(status().isOk())
                .andReturn();
        String secondo = campo(rinnovo, "refreshToken");

        // Il vecchio non vale più, quello nuovo sì
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json(new RinnovaTokenRequest(primo))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json(new RinnovaTokenRequest(secondo))))
                .andExpect(status().isOk());
    }

    @Test
    void ilLogoutRevocaIlRefreshToken() throws Exception {
        String refresh = campo(registra("mario", "mario@test.it"), "refreshToken");

        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content(json(new RinnovaTokenRequest(refresh))))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json(new RinnovaTokenRequest(refresh))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenFalsoRestituisce401() throws Exception {
        mockMvc.perform(get("/api/spazi-lavoro").header(AUTORIZZAZIONE, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errore").value("NON_AUTENTICATO"));
    }

    @Test
    void unUtenteBloccatoPerdeSubitoLAccessoAncheConUnTokenValido() throws Exception {
        String accessToken = campo(registra("mario", "mario@test.it"), "accessToken");

        Utente mario = utenteRepository.findByUsername("mario").orElseThrow();
        mario.setAbilitato(false);
        utenteRepository.saveAndFlush(mario);

        mockMvc.perform(get("/api/spazi-lavoro").header(AUTORIZZAZIONE, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ilCambioPasswordRevocaLeSessioniEFaFunzionareLaNuova() throws Exception {
        MvcResult registrazione = registra("mario", "mario@test.it");
        String accessToken = campo(registrazione, "accessToken");
        String refresh = campo(registrazione, "refreshToken");

        mockMvc.perform(patch("/api/utenti/me/password")
                .header(AUTORIZZAZIONE, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new CambiaPasswordRequest(PASSWORD, "NuovaPassword1"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(new LoginRequest("mario", PASSWORD))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(new LoginRequest("mario", "NuovaPassword1"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json(new RinnovaTokenRequest(refresh))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void perEliminareLAccountServeLaPassword() throws Exception {
        String accessToken = campo(registra("mario", "mario@test.it"), "accessToken");

        mockMvc.perform(post("/api/utenti/me/eliminazione")
                .header(AUTORIZZAZIONE, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new ConfermaPasswordRequest("Sbagliata1"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/utenti/me/eliminazione")
                .header(AUTORIZZAZIONE, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new ConfermaPasswordRequest(PASSWORD))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(new LoginRequest("mario", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    private RegistrazioneRequest registrazione(String username, String email, String password) {
        return new RegistrazioneRequest(username, email, password, "Mario", "Rossi");
    }

    private MvcResult registra(String username, String email) throws Exception {
        return mockMvc.perform(post("/api/auth/registrazione")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(registrazione(username, email, PASSWORD))))
                .andExpect(status().isCreated())
                .andReturn();
    }

    /** Legge un campo testuale dal JSON della risposta */
    private String campo(MvcResult esito, String nome) throws Exception {
        return objectMapper.readTree(esito.getResponse().getContentAsString()).get(nome).asText();
    }
}