// controller/IsolamentoDatiTest.java
package org.elis.progettoesempio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.elis.progettoesempio.entity.Budget;
import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.ObiettivoRisparmio;
import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Utente;

import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.repository.BudgetRepository;
import org.elis.progettoesempio.repository.ObiettivoRisparmioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Un utente che è proprietario del proprio spazio non può toccare le risorse di un altro spazio
 * semplicemente usando il suo id nell'indirizzo.
 */
class IsolamentoDatiTest extends ControllerTestBase {

    @Autowired
    private BudgetRepository budgetRepository;
    @Autowired
    private ObiettivoRisparmioRepository obiettivoRepository;

    private SpazioLavoro spazioA;
    private SpazioLavoro spazioB;
    private Utente proprietarioB;
    private Categoria categoriaA;
    private Budget budgetA;
    private ObiettivoRisparmio obiettivoA;

    @BeforeEach
    void preparaDati() {
        spazioA = creaSpazio(TipoSpazio.AZIENDALE);
        spazioB = creaSpazio(TipoSpazio.AZIENDALE);
        Utente proprietarioA = creaUtente("proprietarioa");
        proprietarioB = creaUtente("proprietariob");
        aggiungiMembro(spazioA, proprietarioA, RuoloSpazio.PROPRIETARIO);
        aggiungiMembro(spazioB, proprietarioB, RuoloSpazio.PROPRIETARIO);

        categoriaA = creaCategoria(spazioA, "Privata", TipoTransazione.USCITA);

        Budget budget = new Budget();
        budget.setSpazioLavoro(spazioA);
        budget.setCategoria(categoriaA);
        budget.setImporto(new BigDecimal("300.00"));
        budget.setMese(10);
        budget.setAnno(2026);
        budgetA = budgetRepository.save(budget);

        ObiettivoRisparmio obiettivo = new ObiettivoRisparmio();
        obiettivo.setSpazioLavoro(spazioA);
        obiettivo.setNome("Obiettivo riservato");
        obiettivo.setImportoObiettivo(new BigDecimal("1000.00"));
        obiettivoA = obiettivoRepository.save(obiettivo);
    }

    @Test
    void unBudgetAltruiNonSiEliminaCambiandoLId() throws Exception {
        mockMvc.perform(delete("/api/spazi-lavoro/" + spazioB.getId() + "/budget/" + budgetA.getId())
                .header(AUTORIZZAZIONE, bearer(proprietarioB)))
                .andExpect(status().isNotFound());

        assertThat(budgetRepository.findById(budgetA.getId())).isPresent();
    }

    @Test
    void unObiettivoAltruiNonRiceveVersamentiCambiandoLId() throws Exception {
        mockMvc.perform(patch("/api/spazi-lavoro/" + spazioB.getId() + "/obiettivi/" + obiettivoA.getId() + "/versamenti")
                .header(AUTORIZZAZIONE, bearer(proprietarioB))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"importo\":100}"))
                .andExpect(status().isNotFound());

        assertThat(obiettivoRepository.findById(obiettivoA.getId()).orElseThrow().getImportoAttuale())
                .isEqualByComparingTo("0");
    }

    @Test
    void unaCategoriaAltruiNonSiModificaCambiandoLId() throws Exception {
        mockMvc.perform(put("/api/spazi-lavoro/" + spazioB.getId() + "/categorie/" + categoriaA.getId())
                .header(AUTORIZZAZIONE, bearer(proprietarioB))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Rubata\",\"tipo\":\"USCITA\",\"colore\":\"#112233\",\"descrizione\":null}"))
                .andExpect(status().isNotFound());

        assertThat(categoriaRepository.findById(categoriaA.getId()).orElseThrow().getNome()).isEqualTo("Privata");
    }
}