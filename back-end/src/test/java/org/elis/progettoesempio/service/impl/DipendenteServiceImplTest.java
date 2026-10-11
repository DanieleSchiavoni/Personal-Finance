// DipendenteServiceImplTest.java
package org.elis.progettoesempio.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.elis.progettoesempio.dto.request.DipendenteRequest;
import org.elis.progettoesempio.dto.request.PagamentoStipendioRequest;
import org.elis.progettoesempio.dto.response.DipendenteResponse;
import org.elis.progettoesempio.dto.response.PagamentoStipendioResponse;
import org.elis.progettoesempio.dto.response.TransazioneResponse;
import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Utente;

import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.exception.AccessoNegatoException;
import org.elis.progettoesempio.exception.ConflittoException;
import org.elis.progettoesempio.exception.OperazioneNonValidaException;
import org.elis.progettoesempio.mapper.BudgetMapper;
import org.elis.progettoesempio.mapper.DipendenteMapper;
import org.elis.progettoesempio.mapper.NotificaMapper;
import org.elis.progettoesempio.mapper.TransazioneMapper;
import org.elis.progettoesempio.repository.CategoriaRepository;
import org.elis.progettoesempio.repository.MembroSpazioRepository;
import org.elis.progettoesempio.repository.SpazioLavoroRepository;
import org.elis.progettoesempio.repository.UtenteRepository;
import org.elis.progettoesempio.service.DipendenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({ DipendenteServiceImpl.class, BudgetServiceImpl.class, AccessoSpazioServiceImpl.class,
        NotificaServiceImpl.class, DipendenteMapper.class, TransazioneMapper.class, BudgetMapper.class,
        NotificaMapper.class })
class DipendenteServiceImplTest {

    private static final LocalDate DATA = LocalDate.of(2026, 10, 6);

    @Autowired
    private DipendenteService dipendenteService;
    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private SpazioLavoroRepository spazioLavoroRepository;
    @Autowired
    private MembroSpazioRepository membroSpazioRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;

    private SpazioLavoro spazio;
    private Utente proprietario;
    private Utente membro;

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

        // Nei test Flyway è disattivato: la categoria globale "Stipendi" va creata qui
        Categoria stipendi = new Categoria();
        stipendi.setNome("Stipendi");
        stipendi.setTipo(TipoTransazione.USCITA);
        stipendi.setTipoSpazio(TipoSpazio.AZIENDALE);
        categoriaRepository.save(stipendi);
    }

    @Test
    void ilPagamentoCreaUnaTransazioneDiUscitaNellaCategoriaStipendi() {
        DipendenteResponse dipendente = creaDipendente("Mario", "Rossi");

        TransazioneResponse pagamento = dipendenteService.registraPagamentoStipendio(
                spazio.getId(), proprietario.getId(), dipendente.id(),
                new PagamentoStipendioRequest(10, 2026, DATA, null, null));

        // Senza importo indicato si usa lo stipendio mensile
        assertThat(pagamento.importo()).isEqualByComparingTo("1500.00");
        assertThat(pagamento.tipo()).isEqualTo(TipoTransazione.USCITA);
        assertThat(pagamento.categoriaNome()).isEqualTo("Stipendi");
        assertThat(pagamento.descrizione()).contains("Mario Rossi");
    }

    @Test
    void lo_stipendioDelloStessoMeseNonSiPuoRegistrareDueVolte() {
        DipendenteResponse dipendente = creaDipendente("Mario", "Rossi");
        PagamentoStipendioRequest richiesta = new PagamentoStipendioRequest(10, 2026, DATA, null, null);
        dipendenteService.registraPagamentoStipendio(spazio.getId(), proprietario.getId(), dipendente.id(), richiesta);

        assertThatThrownBy(() -> dipendenteService.registraPagamentoStipendio(
                spazio.getId(), proprietario.getId(), dipendente.id(), richiesta))
                .isInstanceOf(ConflittoException.class);
    }

    @Test
    void perUnDipendenteNonAttivoNonSiRegistranoPagamenti() {
        DipendenteResponse dipendente = creaDipendente("Mario", "Rossi");
        dipendenteService.aggiorna(spazio.getId(), proprietario.getId(), dipendente.id(),
                new DipendenteRequest("Mario", "Rossi", "Sviluppatore", new BigDecimal("1500.00"), DATA, false));

        assertThatThrownBy(() -> dipendenteService.registraPagamentoStipendio(
                spazio.getId(), proprietario.getId(), dipendente.id(),
                new PagamentoStipendioRequest(10, 2026, DATA, null, null)))
                .isInstanceOf(OperazioneNonValidaException.class);
    }

    @Test
    void ilMembroNonAccedeAiDatiDeiDipendenti() {
        creaDipendente("Mario", "Rossi");

        assertThatThrownBy(() -> dipendenteService.elenca(spazio.getId(), membro.getId()))
                .isInstanceOf(AccessoNegatoException.class);
    }

    @Test
    void soloIlDipendenteSenzaPagamentiSiElimina() {
        DipendenteResponse conPagamenti = creaDipendente("Mario", "Rossi");
        DipendenteResponse senzaPagamenti = creaDipendente("Anna", "Bianchi");
        dipendenteService.registraPagamentoStipendio(spazio.getId(), proprietario.getId(), conPagamenti.id(),
                new PagamentoStipendioRequest(10, 2026, DATA, null, null));

        assertThatThrownBy(() -> dipendenteService.elimina(spazio.getId(), proprietario.getId(), conPagamenti.id()))
                .isInstanceOf(ConflittoException.class);
        assertThatCode(() -> dipendenteService.elimina(spazio.getId(), proprietario.getId(), senzaPagamenti.id()))
                .doesNotThrowAnyException();
    }

    @Test
    void ipagamentiSiElencanoDalPiuRecente() {
        DipendenteResponse dipendente = creaDipendente("Mario", "Rossi");
        dipendenteService.registraPagamentoStipendio(spazio.getId(), proprietario.getId(), dipendente.id(),
                new PagamentoStipendioRequest(10, 2026, DATA, null, null));
        dipendenteService.registraPagamentoStipendio(spazio.getId(), proprietario.getId(), dipendente.id(),
                new PagamentoStipendioRequest(11, 2026, DATA, null, null));

        List<PagamentoStipendioResponse> pagamenti =
                dipendenteService.elencaPagamenti(spazio.getId(), proprietario.getId(), dipendente.id());

        assertThat(pagamenti).hasSize(2);
        assertThat(pagamenti.get(0).mese()).isEqualTo(11);
        assertThat(pagamenti.get(1).mese()).isEqualTo(10);
    }

    private DipendenteResponse creaDipendente(String nome, String cognome) {
        return dipendenteService.crea(spazio.getId(), proprietario.getId(),
                new DipendenteRequest(nome, cognome, "Sviluppatore", new BigDecimal("1500.00"),
                        LocalDate.of(2025, 1, 1), null));
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