// controller/DipendenteControllerTest.java
package org.elis.progettoesempio.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.Dipendente;
import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Utente;

import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.repository.DipendenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class DipendenteControllerTest extends ControllerTestBase {

    @Autowired
    private DipendenteRepository dipendenteRepository;

    private SpazioLavoro spazio;
    private Utente proprietario;
    private Dipendente mario;

    @BeforeEach
    void preparaDati() {
        spazio = creaSpazio(TipoSpazio.AZIENDALE);
        proprietario = creaUtente("proprietario");
        aggiungiMembro(spazio, proprietario, RuoloSpazio.PROPRIETARIO);

        // Nei test Flyway è disattivato: la categoria globale "Stipendi" va creata qui
        Categoria stipendi = new Categoria();
        stipendi.setNome("Stipendi");
        stipendi.setTipo(TipoTransazione.USCITA);
        stipendi.setTipoSpazio(TipoSpazio.AZIENDALE);
        categoriaRepository.save(stipendi);

        mario = new Dipendente();
        mario.setSpazioLavoro(spazio);
        mario.setNome("Mario");
        mario.setCognome("Rossi");
        mario.setStipendioMensile(new BigDecimal("1500.00"));
        dipendenteRepository.save(mario);
    }

    @Test
    void ilProprietarioCreaUnDipendente() throws Exception {
        String corpo = "{\"nome\":\"Anna\",\"cognome\":\"Bianchi\",\"mansione\":\"Contabile\","
                + "\"stipendioMensile\":1800,\"dataAssunzione\":\"2025-01-15\",\"attivo\":true}";

        mockMvc.perform(post(percorso()).header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cognome").value("Bianchi"))
                .andExpect(jsonPath("$.attivo").value(true));
    }

    @Test
    void ilPagamentoCreaUnaTransazioneEUnSecondoPagamentoDelMeseEBloccato() throws Exception {
        String pagamento = percorso() + "/" + mario.getId() + "/pagamenti-stipendio";
        String corpo = "{\"mese\":10,\"anno\":2026}"; // importo, data e metodo hanno valori predefiniti

        mockMvc.perform(post(pagamento).header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoriaNome").value("Stipendi"))
                .andExpect(jsonPath("$.tipo").value("USCITA"))
                .andExpect(jsonPath("$.importo").value(1500.0));

        mockMvc.perform(post(pagamento).header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errore").value("CONFLITTO"));
    }

    @Test
    void unDipendenteConPagamentiNonSiElimina() throws Exception {
        mockMvc.perform(post(percorso() + "/" + mario.getId() + "/pagamenti-stipendio")
                .header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON).content("{\"mese\":10,\"anno\":2026}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete(percorso() + "/" + mario.getId()).header(AUTORIZZAZIONE, bearer(proprietario)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errore").value("CONFLITTO"));
    }

    private String percorso() {
        return "/api/spazi-lavoro/" + spazio.getId() + "/dipendenti";
    }
}