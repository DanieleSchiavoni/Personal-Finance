// TransazioneControllerTest.java
package org.elis.progettoesempio.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.elis.progettoesempio.dto.request.TransazioneRequest;
import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Transazione;
import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.entity.enumerazioni.MetodoPagamento;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class TransazioneControllerTest extends ControllerTestBase {

    private SpazioLavoro spazio;
    private SpazioLavoro altroSpazio;
    private Utente proprietario;
    private Utente lettore;
    private Utente altroUtente;
    private Categoria ristoranti;

    @BeforeEach
    void preparaDati() {
        spazio = creaSpazio(TipoSpazio.AZIENDALE);
        altroSpazio = creaSpazio(TipoSpazio.AZIENDALE);
        proprietario = creaUtente("proprietario");
        lettore = creaUtente("lettore");
        altroUtente = creaUtente("altroutente");
        aggiungiMembro(spazio, proprietario, RuoloSpazio.PROPRIETARIO);
        aggiungiMembro(spazio, lettore, RuoloSpazio.LETTORE);
        aggiungiMembro(altroSpazio, altroUtente, RuoloSpazio.PROPRIETARIO);
        ristoranti = creaCategoria(spazio, "Ristoranti", TipoTransazione.USCITA);
    }

    @Test
    void creareUnaTransazioneRestituisce201() throws Exception {
        TransazioneRequest richiesta = new TransazioneRequest(new BigDecimal("45.50"), "Spesa supermercato",
                LocalDate.of(2026, 10, 6), TipoTransazione.USCITA, MetodoPagamento.CARTA, ristoranti.getId(),
                "Spesa settimanale");

        mockMvc.perform(post(percorso(spazio))
                .header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(richiesta)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importo").value(45.50))
                .andExpect(jsonPath("$.categoriaNome").value("Ristoranti"))
                .andExpect(jsonPath("$.data").value("2026-10-06"));
    }

    @Test
    void unaRichiestaNonValidaRestituisce400ConIDettagliDeiCampi() throws Exception {
        TransazioneRequest richiesta = new TransazioneRequest(new BigDecimal("-5"), "",
                LocalDate.of(2026, 10, 6), TipoTransazione.USCITA, MetodoPagamento.CARTA, ristoranti.getId(), null);

        mockMvc.perform(post(percorso(spazio))
                .header(AUTORIZZAZIONE, bearer(proprietario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(richiesta)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errore").value("ERRORE_VALIDAZIONE"))
                .andExpect(jsonPath("$.campi[?(@.campo == 'importo')]").exists())
                .andExpect(jsonPath("$.campi[?(@.campo == 'descrizione')]").exists());
    }

    @Test
    void senzaIdentitaLaRispostaE401() throws Exception {
        mockMvc.perform(get(percorso(spazio)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errore").value("NON_AUTENTICATO"));
    }

    @Test
    void ilLettoreNonPuoCreareTransazioni() throws Exception {
        TransazioneRequest richiesta = new TransazioneRequest(new BigDecimal("10.00"), "Caffè",
                LocalDate.of(2026, 10, 6), TipoTransazione.USCITA, MetodoPagamento.CONTANTI, ristoranti.getId(), null);

        mockMvc.perform(post(percorso(spazio))
                .header(AUTORIZZAZIONE, bearer(lettore))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(richiesta)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errore").value("ACCESSO_NEGATO"));
    }

    @Test
    void unaTransazioneDiUnAltroSpazioNonSiTrova() throws Exception {
        Transazione transazione = salvaTransazione(TipoTransazione.USCITA, "45.50", "Pranzo");

        // Un utente di un altro spazio: indicando lo spazio della transazione...
        mockMvc.perform(get(percorso(spazio) + "/" + transazione.getId()).header(AUTORIZZAZIONE, bearer(altroUtente)))
                .andExpect(status().isNotFound());
        // ...oppure indicando il proprio spazio con l'id della transazione altrui
        mockMvc.perform(get(percorso(altroSpazio) + "/" + transazione.getId()).header(AUTORIZZAZIONE, bearer(altroUtente)))
                .andExpect(status().isNotFound());
    }

    @Test
    void laRicercaFiltraEPagina() throws Exception {
        salvaTransazione(TipoTransazione.USCITA, "45.50", "Pranzo");
        salvaTransazione(TipoTransazione.USCITA, "20.00", "Cena");
        salvaTransazione(TipoTransazione.ENTRATA, "1000.00", "Vendita");

        mockMvc.perform(get(percorso(spazio))
                .header(AUTORIZZAZIONE, bearer(proprietario))
                .param("tipo", "USCITA")
                .param("dimensione", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(1))
                .andExpect(jsonPath("$.totaleElementi").value(2))
                .andExpect(jsonPath("$.totalePagine").value(2));
    }

    @Test
    void eliminareUnaTransazioneRestituisce204() throws Exception {
        Transazione transazione = salvaTransazione(TipoTransazione.USCITA, "45.50", "Pranzo");

        mockMvc.perform(delete(percorso(spazio) + "/" + transazione.getId()).header(AUTORIZZAZIONE, bearer(proprietario)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(percorso(spazio) + "/" + transazione.getId()).header(AUTORIZZAZIONE, bearer(proprietario)))
                .andExpect(status().isNotFound());
    }

    private String percorso(SpazioLavoro dello) {
        return "/api/spazi-lavoro/" + dello.getId() + "/transazioni";
    }

    private Transazione salvaTransazione(TipoTransazione tipo, String importo, String descrizione) {
        Categoria categoria = tipo == TipoTransazione.USCITA
                ? ristoranti
                : creaCategoria(spazio, "Vendite", TipoTransazione.ENTRATA);

        Transazione transazione = new Transazione();
        transazione.setImporto(new BigDecimal(importo));
        transazione.setDescrizione(descrizione);
        transazione.setDataTransazione(LocalDate.of(2026, 10, 6));
        transazione.setTipo(tipo);
        transazione.setMetodoPagamento(MetodoPagamento.CARTA);
        transazione.setSpazioLavoro(spazio);
        transazione.setCategoria(categoria);
        return transazioneRepository.save(transazione);
    }
}