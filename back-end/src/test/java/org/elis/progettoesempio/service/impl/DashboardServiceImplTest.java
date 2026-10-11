package org.elis.progettoesempio.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.elis.progettoesempio.dto.response.PuntoTrendResponse;
import org.elis.progettoesempio.dto.response.RiepilogoDashboardResponse;
import org.elis.progettoesempio.dto.response.TotaleCategoriaResponse;
import org.elis.progettoesempio.entity.Budget;
import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.Dipendente;
import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.ObiettivoRisparmio;
import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Transazione;
import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.entity.enumerazioni.Granularita;
import org.elis.progettoesempio.entity.enumerazioni.MetodoPagamento;

import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.exception.OperazioneNonValidaException;
import org.elis.progettoesempio.exception.RisorsaNonTrovataException;
import org.elis.progettoesempio.mapper.BudgetMapper;
import org.elis.progettoesempio.mapper.DashboardMapper;
import org.elis.progettoesempio.mapper.NotificaMapper;
import org.elis.progettoesempio.mapper.TransazioneMapper;
import org.elis.progettoesempio.repository.BudgetRepository;
import org.elis.progettoesempio.repository.CategoriaRepository;
import org.elis.progettoesempio.repository.DipendenteRepository;
import org.elis.progettoesempio.repository.MembroSpazioRepository;
import org.elis.progettoesempio.repository.ObiettivoRisparmioRepository;
import org.elis.progettoesempio.repository.SpazioLavoroRepository;
import org.elis.progettoesempio.repository.TransazioneRepository;
import org.elis.progettoesempio.repository.UtenteRepository;
import org.elis.progettoesempio.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({ DashboardServiceImpl.class, TransazioneServiceImpl.class, BudgetServiceImpl.class,
        AccessoSpazioServiceImpl.class, NotificaServiceImpl.class, DashboardMapper.class,
        TransazioneMapper.class, BudgetMapper.class, NotificaMapper.class })
class DashboardServiceImplTest {

    @Autowired
    private DashboardService dashboardService;
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
    private BudgetRepository budgetRepository;
    @Autowired
    private ObiettivoRisparmioRepository obiettivoRepository;
    @Autowired
    private DipendenteRepository dipendenteRepository;

    private SpazioLavoro spazio;
    private Utente proprietario;
    private Utente membro;
    private Utente estraneo;
    private Categoria vendite;
    private Categoria ristoranti;
    private Categoria casa;
    private Categoria stipendi;

    @BeforeEach
    void preparaDati() {
        spazio = new SpazioLavoro();
        spazio.setNome("Azienda di prova");
        spazio.setTipo(TipoSpazio.AZIENDALE);
        spazioLavoroRepository.save(spazio);

        proprietario = creaUtente("proprietario");
        membro = creaUtente("membro");
        estraneo = creaUtente("estraneo");
        aggiungiMembro(proprietario, RuoloSpazio.PROPRIETARIO);
        aggiungiMembro(membro, RuoloSpazio.MEMBRO);

        vendite = creaCategoria("Vendite", TipoTransazione.ENTRATA);
        ristoranti = creaCategoria("Ristoranti", TipoTransazione.USCITA);
        casa = creaCategoria("Casa", TipoTransazione.USCITA);
        stipendi = creaCategoria("Stipendi", TipoTransazione.USCITA);
    }

    @Test
    void ilRiepilogoCalcolaSaldoEntrateEUsciteDelMese() {
        salva(vendite, TipoTransazione.ENTRATA, "300.00", LocalDate.of(2026, 9, 15)); // mese precedente
        salva(vendite, TipoTransazione.ENTRATA, "2000.00", LocalDate.of(2026, 10, 3));
        salva(ristoranti, TipoTransazione.USCITA, "500.00", LocalDate.of(2026, 10, 5));

        RiepilogoDashboardResponse riepilogo = dashboardService.riepilogo(spazio.getId(), proprietario.getId(), 10, 2026);

        assertThat(riepilogo.saldoTotale()).isEqualByComparingTo("1800.00");
        assertThat(riepilogo.entrateMese()).isEqualByComparingTo("2000.00");
        assertThat(riepilogo.usciteMese()).isEqualByComparingTo("500.00");
        assertThat(riepilogo.risparmioMese()).isEqualByComparingTo("1500.00");
        assertThat(riepilogo.transazioniMese()).isEqualTo(2);
        assertThat(riepilogo.ultimeTransazioni()).hasSize(3);
    }

    @Test
    void ilRiepilogoSintetizzaBudgetEObiettivi() {
        salva(ristoranti, TipoTransazione.USCITA, "150.00", LocalDate.of(2026, 10, 5));

        Budget budget = new Budget();
        budget.setSpazioLavoro(spazio);
        budget.setCategoria(ristoranti);
        budget.setImporto(new BigDecimal("300.00"));
        budget.setMese(10);
        budget.setAnno(2026);
        budgetRepository.save(budget);

        salvaObiettivo("Vacanza", "1000.00", "250.00");
        salvaObiettivo("Computer", "500.00", "500.00"); // già raggiunto

        RiepilogoDashboardResponse riepilogo = dashboardService.riepilogo(spazio.getId(), proprietario.getId(), 10, 2026);

        assertThat(riepilogo.budget().totale()).isEqualByComparingTo("300.00");
        assertThat(riepilogo.budget().speso()).isEqualByComparingTo("150.00");
        assertThat(riepilogo.budget().percentuale()).isEqualByComparingTo("50.0");
        assertThat(riepilogo.obiettivi().totali()).isEqualTo(2);
        assertThat(riepilogo.obiettivi().completati()).isEqualTo(1);
        assertThat(riepilogo.obiettivi().importoRisparmiato()).isEqualByComparingTo("750.00");
    }

    @Test
    void ilTrendMensileRiempieIMesiVuotiECalcolaIlSaldoProgressivo() {
        salva(vendite, TipoTransazione.ENTRATA, "500.00", LocalDate.of(2026, 7, 10)); // saldo iniziale
        salva(ristoranti, TipoTransazione.USCITA, "100.00", LocalDate.of(2026, 8, 20));
        salva(vendite, TipoTransazione.ENTRATA, "1000.00", LocalDate.of(2026, 10, 3));

        List<PuntoTrendResponse> punti = dashboardService.trend(spazio.getId(), proprietario.getId(),
                Granularita.MESE, LocalDate.of(2026, 8, 15), LocalDate.of(2026, 10, 20));

        assertThat(punti).hasSize(3);
        assertThat(punti.get(0).etichetta()).isEqualTo("2026-08");
        assertThat(punti.get(0).uscite()).isEqualByComparingTo("100.00");
        assertThat(punti.get(0).saldoProgressivo()).isEqualByComparingTo("400.00");
        assertThat(punti.get(1).etichetta()).isEqualTo("2026-09"); // mese senza movimenti
        assertThat(punti.get(1).entrate()).isEqualByComparingTo("0");
        assertThat(punti.get(1).saldoProgressivo()).isEqualByComparingTo("400.00");
        assertThat(punti.get(2).saldoProgressivo()).isEqualByComparingTo("1400.00");
    }

    @Test
    void ilTrendSettimanaleUsaSettimaneDaLunedi() {
        salva(ristoranti, TipoTransazione.USCITA, "50.00", LocalDate.of(2026, 10, 4)); // domenica
        salva(vendite, TipoTransazione.ENTRATA, "200.00", LocalDate.of(2026, 10, 5)); // lunedì

        List<PuntoTrendResponse> punti = dashboardService.trend(spazio.getId(), proprietario.getId(),
                Granularita.SETTIMANA, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 11));

        assertThat(punti).hasSize(2);
        assertThat(punti.get(0).inizio()).isEqualTo(LocalDate.of(2026, 9, 28));
        assertThat(punti.get(0).uscite()).isEqualByComparingTo("50.00");
        assertThat(punti.get(1).inizio()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(punti.get(1).entrate()).isEqualByComparingTo("200.00");
    }

    @Test
    void leSpesePerCategoriaHannoLaPercentualeSulTotale() {
        salva(ristoranti, TipoTransazione.USCITA, "75.00", LocalDate.of(2026, 10, 5));
        salva(casa, TipoTransazione.USCITA, "25.00", LocalDate.of(2026, 10, 6));

        List<TotaleCategoriaResponse> categorie = dashboardService.spesePerCategoria(
                spazio.getId(), proprietario.getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(categorie).hasSize(2);
        assertThat(categorie.get(0).categoriaNome()).isEqualTo("Ristoranti"); // la più alta per prima
        assertThat(categorie.get(0).percentuale()).isEqualByComparingTo("75.0");
        assertThat(categorie.get(1).percentuale()).isEqualByComparingTo("25.0");
    }

    @Test
    void ilMembroNonVedeIPagamentiDiStipendioMaITotaliLiIncludono() {
        Dipendente dipendente = new Dipendente();
        dipendente.setSpazioLavoro(spazio);
        dipendente.setNome("Mario");
        dipendente.setCognome("Rossi");
        dipendente.setStipendioMensile(new BigDecimal("1500.00"));
        dipendenteRepository.save(dipendente);

        Transazione stipendio = costruisci(stipendi, TipoTransazione.USCITA, "1500.00", LocalDate.of(2026, 10, 5));
        stipendio.setDipendente(dipendente);
        transazioneRepository.save(stipendio);

        RiepilogoDashboardResponse vistaMembro = dashboardService.riepilogo(spazio.getId(), membro.getId(), 10, 2026);

        assertThat(vistaMembro.ultimeTransazioni()).isEmpty();
        assertThat(vistaMembro.usciteMese()).isEqualByComparingTo("1500.00");
    }

    @Test
    void unEstraneoNonAccedeAllaDashboard() {
        assertThatThrownBy(() -> dashboardService.riepilogo(spazio.getId(), estraneo.getId(), 10, 2026))
                .isInstanceOf(RisorsaNonTrovataException.class);
    }

    @Test
    void unIntervalloTroppoAmpioVieneRifiutato() {
        assertThatThrownBy(() -> dashboardService.trend(spazio.getId(), proprietario.getId(),
                Granularita.SETTIMANA, LocalDate.of(2000, 1, 1), LocalDate.of(2026, 10, 11)))
                .isInstanceOf(OperazioneNonValidaException.class);
    }

    private Transazione costruisci(Categoria categoria, TipoTransazione tipo, String importo, LocalDate data) {
        Transazione transazione = new Transazione();
        transazione.setImporto(new BigDecimal(importo));
        transazione.setDescrizione("Movimento di prova");
        transazione.setDataTransazione(data);
        transazione.setTipo(tipo);
        transazione.setMetodoPagamento(MetodoPagamento.CARTA);
        transazione.setSpazioLavoro(spazio);
        transazione.setCategoria(categoria);
        return transazione;
    }

    private void salva(Categoria categoria, TipoTransazione tipo, String importo, LocalDate data) {
        transazioneRepository.save(costruisci(categoria, tipo, importo, data));
    }

    private void salvaObiettivo(String nome, String importoObiettivo, String importoAttuale) {
        ObiettivoRisparmio obiettivo = new ObiettivoRisparmio();
        obiettivo.setSpazioLavoro(spazio);
        obiettivo.setNome(nome);
        obiettivo.setImportoObiettivo(new BigDecimal(importoObiettivo));
        obiettivo.setImportoAttuale(new BigDecimal(importoAttuale));
        obiettivoRepository.save(obiettivo);
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