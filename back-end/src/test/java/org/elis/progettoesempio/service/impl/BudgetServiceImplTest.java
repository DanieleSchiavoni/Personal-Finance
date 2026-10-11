// BudgetServiceImplTest.java
package org.elis.progettoesempio.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.elis.progettoesempio.dto.request.BudgetRequest;
import org.elis.progettoesempio.dto.response.BudgetResponse;
import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Transazione;
import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.entity.enumerazioni.MetodoPagamento;
import org.elis.progettoesempio.entity.enumerazioni.StatoBudget;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.exception.ConflittoException;
import org.elis.progettoesempio.exception.OperazioneNonValidaException;
import org.elis.progettoesempio.mapper.BudgetMapper;
import org.elis.progettoesempio.mapper.NotificaMapper;
import org.elis.progettoesempio.repository.CategoriaRepository;
import org.elis.progettoesempio.repository.MembroSpazioRepository;
import org.elis.progettoesempio.repository.NotificaRepository;
import org.elis.progettoesempio.repository.SpazioLavoroRepository;
import org.elis.progettoesempio.repository.TransazioneRepository;
import org.elis.progettoesempio.repository.UtenteRepository;
import org.elis.progettoesempio.service.BudgetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({ BudgetServiceImpl.class, AccessoSpazioServiceImpl.class, NotificaServiceImpl.class,
        BudgetMapper.class, NotificaMapper.class })
class BudgetServiceImplTest {

    private static final LocalDate DATA = LocalDate.of(2026, 10, 6);

    @Autowired
    private BudgetService budgetService;
    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private SpazioLavoroRepository spazioLavoroRepository;
    @Autowired
    private MembroSpazioRepository membroSpazioRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private TransazioneRepository transazioneRepository;
    @Autowired
    private NotificaRepository notificaRepository;

    private SpazioLavoro spazio;
    private Utente proprietario;
    private Utente membro;
    private Categoria ristoranti;
    private Categoria vendite;

    @BeforeEach
    void preparaDati() {
        spazio = new SpazioLavoro();
        spazio.setNome("Azienda di prova");
        spazio.setTipo(TipoSpazio.AZIENDALE);
        spazioLavoroRepository.save(spazio);

        proprietario = creaUtente("proprietario");
        membro = creaUtente("membro");
        aggiungiMembro(proprietario, RuoloSpazio.PROPRIETARIO);
        aggiungiMembro(membro, RuoloSpazio.MEMBRO);

        ristoranti = creaCategoria("Ristoranti", TipoTransazione.USCITA);
        vendite = creaCategoria("Vendite", TipoTransazione.ENTRATA);
    }

    @Test
    void calcolaSpesoDisponibilePercentualeEStato() {
        budgetService.crea(spazio.getId(), proprietario.getId(),
                new BudgetRequest(ristoranti.getId(), new BigDecimal("300.00"), 10, 2026));
        salvaSpesa("245.00");

        BudgetResponse budget = budgetDelMese();

        assertThat(budget.speso()).isEqualByComparingTo("245.00");
        assertThat(budget.disponibile()).isEqualByComparingTo("55.00");
        assertThat(budget.percentuale()).isEqualByComparingTo("81.6");
        assertThat(budget.stato()).isEqualTo(StatoBudget.ATTENZIONE);
    }

    @Test
    void sottoLaSogliaLoStatoEOk() {
        budgetService.crea(spazio.getId(), proprietario.getId(),
                new BudgetRequest(ristoranti.getId(), new BigDecimal("300.00"), 10, 2026));
        salvaSpesa("100.00");

        assertThat(budgetDelMese().stato()).isEqualTo(StatoBudget.OK);
    }

    @Test
    void oltreIlLimiteLoStatoESuperato() {
        budgetService.crea(spazio.getId(), proprietario.getId(),
                new BudgetRequest(ristoranti.getId(), new BigDecimal("300.00"), 10, 2026));
        salvaSpesa("310.00");

        BudgetResponse budget = budgetDelMese();

        assertThat(budget.stato()).isEqualTo(StatoBudget.SUPERATO);
        assertThat(budget.disponibile()).isEqualByComparingTo("-10.00");
    }

    @Test
    void unBudgetDuplicatoVieneRifiutato() {
        BudgetRequest richiesta = new BudgetRequest(ristoranti.getId(), new BigDecimal("300.00"), 10, 2026);
        budgetService.crea(spazio.getId(), proprietario.getId(), richiesta);

        assertThatThrownBy(() -> budgetService.crea(spazio.getId(), proprietario.getId(), richiesta))
                .isInstanceOf(ConflittoException.class);
    }

    @Test
    void ilBudgetSiImpostaSoloSuCategorieDiUscita() {
        BudgetRequest richiesta = new BudgetRequest(vendite.getId(), new BigDecimal("300.00"), 10, 2026);

        assertThatThrownBy(() -> budgetService.crea(spazio.getId(), proprietario.getId(), richiesta))
                .isInstanceOf(OperazioneNonValidaException.class);
    }

    @Test
    void ilSuperamentoDellaSogliaNotificaSoloChiGestisceLoSpazio() {
        budgetService.crea(spazio.getId(), proprietario.getId(),
                new BudgetRequest(ristoranti.getId(), new BigDecimal("300.00"), 10, 2026));

        salvaSpesa("200.00"); // 66,6%: nessuna soglia
        salvaSpesa("45.00"); // 81,6%: scatta ATTENZIONE
        budgetService.controllaSoglie(spazio.getId(), ristoranti.getId(), DATA, new BigDecimal("45.00"));

        salvaSpesa("10.00"); // 85%: resta ATTENZIONE, nessuna nuova notifica
        budgetService.controllaSoglie(spazio.getId(), ristoranti.getId(), DATA, new BigDecimal("10.00"));

        assertThat(notificaRepository.findByUtenteIdOrderByDataCreazioneDesc(proprietario.getId())).hasSize(1);
        assertThat(notificaRepository.countByUtenteIdAndLettaFalse(membro.getId())).isZero();
    }

    private BudgetResponse budgetDelMese() {
        List<BudgetResponse> budget = budgetService.elenca(spazio.getId(), proprietario.getId(), 10, 2026);
        assertThat(budget).hasSize(1);
        return budget.get(0);
    }

    private void salvaSpesa(String importo) {
        Transazione spesa = new Transazione();
        spesa.setImporto(new BigDecimal(importo));
        spesa.setDescrizione("Spesa di prova");
        spesa.setDataTransazione(DATA);
        spesa.setTipo(TipoTransazione.USCITA);
        spesa.setMetodoPagamento(MetodoPagamento.CARTA);
        spesa.setSpazioLavoro(spazio);
        spesa.setCategoria(ristoranti);
        transazioneRepository.save(spesa);
    }

    private Categoria creaCategoria(String nome, TipoTransazione tipo) {
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setTipo(tipo);
        categoria.setSpazioLavoro(spazio);
        return categoriaRepository.save(categoria);
    }

    private Utente creaUtente(String nome) {
        Utente utente = new Utente();
        utente.setUsername(nome);
        utente.setEmail(nome + "@test.it");
        utente.setPassword("password-di-prova");
        utente.setNome(nome);
        utente.setCognome("Test");
        return utenteRepository.save(utente);
    }

    private void aggiungiMembro(Utente utente, RuoloSpazio ruolo) {
        MembroSpazio appartenenza = new MembroSpazio();
        appartenenza.setSpazioLavoro(spazio);
        appartenenza.setUtente(utente);
        appartenenza.setRuolo(ruolo);
        membroSpazioRepository.save(appartenenza);
    }
}