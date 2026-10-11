// AdminServiceImplTest.java
package org.elis.progettoesempio.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.elis.progettoesempio.dto.request.CategoriaGlobaleRequest;
import org.elis.progettoesempio.dto.request.RicercaUtentiRequest;
import org.elis.progettoesempio.dto.response.CategoriaGlobaleResponse;
import org.elis.progettoesempio.dto.response.PaginaResponse;
import org.elis.progettoesempio.dto.response.StatisticheGeneraliResponse;
import org.elis.progettoesempio.dto.response.UtenteResponse;
import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.RefreshToken;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Transazione;
import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.entity.enumerazioni.MetodoPagamento;
import org.elis.progettoesempio.entity.enumerazioni.RuoloUtente;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.exception.AccessoNegatoException;
import org.elis.progettoesempio.exception.ConflittoException;
import org.elis.progettoesempio.exception.OperazioneNonValidaException;
import org.elis.progettoesempio.mapper.CategoriaGlobaleMapper;
import org.elis.progettoesempio.mapper.UtenteMapper;
import org.elis.progettoesempio.repository.CategoriaRepository;
import org.elis.progettoesempio.repository.RefreshTokenRepository;
import org.elis.progettoesempio.repository.SpazioLavoroRepository;
import org.elis.progettoesempio.repository.TransazioneRepository;
import org.elis.progettoesempio.repository.UtenteRepository;
import org.elis.progettoesempio.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({ AdminServiceImpl.class, UtenteMapper.class, CategoriaGlobaleMapper.class })
class AdminServiceImplTest {

    @Autowired
    private AdminService adminService;
    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private SpazioLavoroRepository spazioLavoroRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private TransazioneRepository transazioneRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private Utente admin;
    private Utente mario;

    @BeforeEach
    void preparaDati() {
        admin = creaUtente("admin", RuoloUtente.ADMIN, true);
        mario = creaUtente("mario", RuoloUtente.UTENTE, true);
    }

    @Test
    void unUtenteComuneNonUsaLeFunzioniAdmin() {
        assertThatThrownBy(() -> adminService.elencaUtenti(mario.getId(), ricerca(null)))
                .isInstanceOf(AccessoNegatoException.class);
        assertThatThrownBy(() -> adminService.statistiche(mario.getId()))
                .isInstanceOf(AccessoNegatoException.class);
    }

    @Test
    void laRicercaTrovaGliUtentiPerTesto() {
        creaUtente("luca", RuoloUtente.UTENTE, true);

        PaginaResponse<UtenteResponse> risultato = adminService.elencaUtenti(admin.getId(), ricerca("mar"));

        assertThat(risultato.contenuto()).hasSize(1);
        assertThat(risultato.contenuto().get(0).username()).isEqualTo("mario");
    }

    @Test
    void bloccareUnUtenteRevocaIlSuoRefreshToken() {
        RefreshToken token = new RefreshToken();
        token.setToken("token-di-prova");
        token.setDataScadenza(LocalDateTime.now().plusDays(7));
        token.setUtente(mario);
        refreshTokenRepository.save(token);

        UtenteResponse bloccato = adminService.impostaAbilitazione(admin.getId(), mario.getId(), false);

        assertThat(bloccato.abilitato()).isFalse();
        assertThat(refreshTokenRepository.findByToken("token-di-prova")).isEmpty();
        assertThat(utenteRepository.findById(mario.getId()).orElseThrow().isAbilitato()).isFalse();
    }

    @Test
    void unAdminNonPuoBloccareSeStesso() {
        assertThatThrownBy(() -> adminService.impostaAbilitazione(admin.getId(), admin.getId(), false))
                .isInstanceOf(OperazioneNonValidaException.class);
    }

    @Test
    void laCategoriaDiSistemaStipendiNonSiModificaNeSiElimina() {
        Categoria stipendi = salvaCategoriaGlobale("Stipendi", TipoTransazione.USCITA);

        assertThatThrownBy(() -> adminService.aggiornaCategoriaGlobale(admin.getId(), stipendi.getId(),
                new CategoriaGlobaleRequest("Paghe", TipoTransazione.USCITA, null, null, TipoSpazio.AZIENDALE)))
                .isInstanceOf(OperazioneNonValidaException.class);
        assertThatThrownBy(() -> adminService.eliminaCategoriaGlobale(admin.getId(), stipendi.getId()))
                .isInstanceOf(OperazioneNonValidaException.class);
    }

    @Test
    void nonSiCreaUnaCategoriaGlobaleDuplicata() {
        CategoriaGlobaleRequest richiesta =
                new CategoriaGlobaleRequest("Marketing", TipoTransazione.USCITA, "#d63384", null, TipoSpazio.AZIENDALE);
        adminService.creaCategoriaGlobale(admin.getId(), richiesta);

        assertThatThrownBy(() -> adminService.creaCategoriaGlobale(admin.getId(), richiesta))
                .isInstanceOf(ConflittoException.class);
    }

    @Test
    void unaCategoriaGlobaleInUsoNonSiElimina() {
        CategoriaGlobaleResponse creata = adminService.creaCategoriaGlobale(admin.getId(),
                new CategoriaGlobaleRequest("Marketing", TipoTransazione.USCITA, null, null, TipoSpazio.AZIENDALE));

        SpazioLavoro spazio = creaSpazio(TipoSpazio.AZIENDALE);
        Transazione uso = new Transazione();
        uso.setImporto(new BigDecimal("50.00"));
        uso.setDescrizione("Campagna");
        uso.setDataTransazione(LocalDate.of(2026, 10, 6));
        uso.setTipo(TipoTransazione.USCITA);
        uso.setMetodoPagamento(MetodoPagamento.CARTA);
        uso.setSpazioLavoro(spazio);
        uso.setCategoria(categoriaRepository.findById(creata.id()).orElseThrow());
        transazioneRepository.save(uso);

        assertThatThrownBy(() -> adminService.eliminaCategoriaGlobale(admin.getId(), creata.id()))
                .isInstanceOf(ConflittoException.class);
    }

    @Test
    void leStatisticheSonoSoloConteggi() {
        creaUtente("bloccato", RuoloUtente.UTENTE, false);
        creaSpazio(TipoSpazio.PERSONALE);
        creaSpazio(TipoSpazio.AZIENDALE);
        salvaCategoriaGlobale("Altro", TipoTransazione.USCITA);

        StatisticheGeneraliResponse statistiche = adminService.statistiche(admin.getId());

        assertThat(statistiche.utentiTotali()).isEqualTo(3);
        assertThat(statistiche.utentiAbilitati()).isEqualTo(2);
        assertThat(statistiche.utentiBloccati()).isEqualTo(1);
        assertThat(statistiche.amministratori()).isEqualTo(1);
        assertThat(statistiche.spaziPersonali()).isEqualTo(1);
        assertThat(statistiche.spaziAziendali()).isEqualTo(1);
        assertThat(statistiche.transazioniTotali()).isZero();
        assertThat(statistiche.categorieGlobali()).isEqualTo(1);
    }

    private RicercaUtentiRequest ricerca(String testo) {
        return new RicercaUtentiRequest(testo, null, null);
    }

    private Categoria salvaCategoriaGlobale(String nome, TipoTransazione tipo) {
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setTipo(tipo);
        categoria.setTipoSpazio(TipoSpazio.AZIENDALE);
        return categoriaRepository.save(categoria);
    }

    private SpazioLavoro creaSpazio(TipoSpazio tipo) {
        SpazioLavoro spazio = new SpazioLavoro();
        spazio.setNome("Spazio di prova");
        spazio.setTipo(tipo);
        return spazioLavoroRepository.save(spazio);
    }

    private Utente creaUtente(String nome, RuoloUtente ruolo, boolean abilitato) {
        Utente utente = new Utente();
        utente.setUsername(nome);
        utente.setEmail(nome + "@test.it");
        utente.setPassword("password-di-prova");
        utente.setNome(nome);
        utente.setCognome("Test");
        utente.setRuolo(ruolo);
        utente.setAbilitato(abilitato);
        return utenteRepository.save(utente);
    }
}