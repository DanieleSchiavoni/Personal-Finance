// src/test/java/org/elis/progettoesempio/service/impl/ReportServiceImplTest.java
package org.elis.progettoesempio.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.Dipendente;
import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Transazione;
import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.entity.enumerazioni.MetodoPagamento;

import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.exception.AccessoNegatoException;
import org.elis.progettoesempio.exception.OperazioneNonValidaException;
import org.elis.progettoesempio.exception.RisorsaNonTrovataException;
import org.elis.progettoesempio.mapper.BudgetMapper;
import org.elis.progettoesempio.mapper.NotificaMapper;
import org.elis.progettoesempio.mapper.TransazioneMapper;
import org.elis.progettoesempio.repository.CategoriaRepository;
import org.elis.progettoesempio.repository.DipendenteRepository;
import org.elis.progettoesempio.repository.MembroSpazioRepository;
import org.elis.progettoesempio.repository.SpazioLavoroRepository;
import org.elis.progettoesempio.repository.TransazioneRepository;
import org.elis.progettoesempio.repository.UtenteRepository;
import org.elis.progettoesempio.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({ ReportServiceImpl.class, GeneratorePdfResoconto.class, AccessoSpazioServiceImpl.class,
        BudgetServiceImpl.class, NotificaServiceImpl.class, TransazioneMapper.class, BudgetMapper.class,
        NotificaMapper.class })
class ReportServiceImplTest {

    @Autowired
    private ReportService reportService;
    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private SpazioLavoroRepository spazioLavoroRepository;
    @Autowired
    private MembroSpazioRepository membroSpazioRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private DipendenteRepository dipendenteRepository;
    @Autowired
    private TransazioneRepository transazioneRepository;

    private SpazioLavoro spazio;
    private Utente proprietario;
    private Utente membro;
    private Utente estraneo;

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

        Categoria vendite = creaCategoria("Vendite", TipoTransazione.ENTRATA);
        Categoria stipendi = creaCategoria("Stipendi", TipoTransazione.USCITA);
        salva(vendite, TipoTransazione.ENTRATA, "2000.00", null);

        Dipendente dipendente = new Dipendente();
        dipendente.setSpazioLavoro(spazio);
        dipendente.setNome("Mario");
        dipendente.setCognome("Rossi");
        dipendente.setStipendioMensile(new BigDecimal("1500.00"));
        dipendenteRepository.save(dipendente);
        salva(stipendi, TipoTransazione.USCITA, "1500.00", dipendente);
    }

    @Test
    void ilProprietarioOttieneUnPdfConStipendiETransazioni() {
        byte[] pdf = reportService.generaResocontoMensile(spazio.getId(), proprietario.getId(), 10, 2026);

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void ilMembroNonPuoScaricareIlResoconto() {
        assertThatThrownBy(() -> reportService.generaResocontoMensile(spazio.getId(), membro.getId(), 10, 2026))
                .isInstanceOf(AccessoNegatoException.class);
    }

    @Test
    void unEstraneoNonVedeLoSpazio() {
        assertThatThrownBy(() -> reportService.generaResocontoMensile(spazio.getId(), estraneo.getId(), 10, 2026))
                .isInstanceOf(RisorsaNonTrovataException.class);
    }

    @Test
    void unMeseNonValidoVieneRifiutato() {
        assertThatThrownBy(() -> reportService.generaResocontoMensile(spazio.getId(), proprietario.getId(), 13, 2026))
                .isInstanceOf(OperazioneNonValidaException.class);
    }

    private void salva(Categoria categoria, TipoTransazione tipo, String importo, Dipendente dipendente) {
        Transazione transazione = new Transazione();
        transazione.setImporto(new BigDecimal(importo));
        transazione.setDescrizione("Movimento di prova");
        transazione.setDataTransazione(LocalDate.of(2026, 10, 5));
        transazione.setTipo(tipo);
        transazione.setMetodoPagamento(MetodoPagamento.BONIFICO);
        transazione.setSpazioLavoro(spazio);
        transazione.setCategoria(categoria);
        transazione.setDipendente(dipendente);
        transazioneRepository.save(transazione);
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