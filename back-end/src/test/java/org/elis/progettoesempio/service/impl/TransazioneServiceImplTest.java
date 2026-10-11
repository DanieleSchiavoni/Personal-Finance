// TransazioneServiceImplTest.java
package org.elis.progettoesempio.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.elis.progettoesempio.dto.request.RicercaTransazioniRequest;
import org.elis.progettoesempio.dto.request.TransazioneRequest;
import org.elis.progettoesempio.dto.response.PaginaResponse;
import org.elis.progettoesempio.dto.response.TransazioneResponse;
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
import org.elis.progettoesempio.service.TransazioneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({ TransazioneServiceImpl.class, BudgetServiceImpl.class, AccessoSpazioServiceImpl.class,
        NotificaServiceImpl.class, TransazioneMapper.class, BudgetMapper.class, NotificaMapper.class })
class TransazioneServiceImplTest {

    private static final LocalDate DATA = LocalDate.of(2026, 10, 6);

    @Autowired
    private TransazioneService transazioneService;
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
    private SpazioLavoro altroSpazio;
    private Utente proprietario;
    private Utente membro;
    private Utente altroUtente;
    private Categoria ristoranti;
    private Categoria vendite;
    private Categoria stipendi;

    @BeforeEach
    void preparaDati() {
        spazio = creaSpazio("Azienda");
        altroSpazio = creaSpazio("Altra azienda");

        proprietario = creaUtente("proprietario");
        membro = creaUtente("membro");
        altroUtente = creaUtente("altroutente");
        aggiungiMembro(spazio, proprietario, RuoloSpazio.PROPRIETARIO);
        aggiungiMembro(spazio, membro, RuoloSpazio.MEMBRO);
        aggiungiMembro(altroSpazio, altroUtente, RuoloSpazio.PROPRIETARIO);

        ristoranti = creaCategoria("Ristoranti", TipoTransazione.USCITA);
        vendite = creaCategoria("Vendite", TipoTransazione.ENTRATA);
        stipendi = creaCategoria("Stipendi", TipoTransazione.USCITA);
    }

    @Test
    void laRicercaFiltraPerTipo() {
        creaComeProprietario(ristoranti, TipoTransazione.USCITA, "45.50", "Pranzo");
        creaComeProprietario(vendite, TipoTransazione.ENTRATA, "1000.00", "Vendita");

        PaginaResponse<TransazioneResponse> risultato = transazioneService.cerca(
                spazio.getId(), proprietario.getId(),
                filtro(TipoTransazione.USCITA, null));

        assertThat(risultato.contenuto()).hasSize(1);
        assertThat(risultato.contenuto().get(0).descrizione()).isEqualTo("Pranzo");
    }

    @Test
    void laRicercaFiltraPerImportoMinimo() {
        creaComeProprietario(ristoranti, TipoTransazione.USCITA, "45.50", "Pranzo");
        creaComeProprietario(vendite, TipoTransazione.ENTRATA, "1000.00", "Vendita");

        PaginaResponse<TransazioneResponse> risultato = transazioneService.cerca(
                spazio.getId(), proprietario.getId(),
                filtro(null, new BigDecimal("100.00")));

        assertThat(risultato.contenuto()).hasSize(1);
        assertThat(risultato.contenuto().get(0).descrizione()).isEqualTo("Vendita");
    }

    @Test
    void ilMembroNonVedeIPagamentiDiStipendio() {
        Transazione stipendio = salvaPagamentoStipendio();

        PaginaResponse<TransazioneResponse> vistaMembro = transazioneService.cerca(
                spazio.getId(), membro.getId(), filtro(null, null));
        PaginaResponse<TransazioneResponse> vistaProprietario = transazioneService.cerca(
                spazio.getId(), proprietario.getId(), filtro(null, null));

        assertThat(vistaMembro.contenuto()).isEmpty();
        assertThat(vistaProprietario.contenuto()).hasSize(1);
        assertThatThrownBy(() -> transazioneService.dettaglio(spazio.getId(), membro.getId(), stipendio.getId()))
                .isInstanceOf(RisorsaNonTrovataException.class);
    }

    @Test
    void ilMembroNonModificaLeTransazioniDegliAltri() {
        TransazioneResponse delProprietario =
                creaComeProprietario(ristoranti, TipoTransazione.USCITA, "45.50", "Pranzo");

        assertThatThrownBy(() -> transazioneService.aggiorna(spazio.getId(), membro.getId(),
                delProprietario.id(), richiesta(ristoranti, TipoTransazione.USCITA, "10.00", "Modificata")))
                .isInstanceOf(AccessoNegatoException.class);
        assertThatThrownBy(() -> transazioneService.elimina(spazio.getId(), membro.getId(), delProprietario.id()))
                .isInstanceOf(AccessoNegatoException.class);
    }

    @Test
    void ilMembroModificaLeProprieTransazioni() {
        TransazioneResponse sua = transazioneService.crea(spazio.getId(), membro.getId(),
                richiesta(ristoranti, TipoTransazione.USCITA, "20.00", "Caffè"));

        assertThatCode(() -> transazioneService.aggiorna(spazio.getId(), membro.getId(), sua.id(),
                richiesta(ristoranti, TipoTransazione.USCITA, "25.00", "Caffè e dolce")))
                .doesNotThrowAnyException();
    }

    @Test
    void unaTransazioneNonSiVedeDaUnAltroSpazio() {
        TransazioneResponse transazione =
                creaComeProprietario(ristoranti, TipoTransazione.USCITA, "45.50", "Pranzo");

        // Un altro utente, che usa l'id di una transazione non sua: nel suo spazio...
        assertThatThrownBy(() -> transazioneService.dettaglio(
                altroSpazio.getId(), altroUtente.getId(), transazione.id()))
                .isInstanceOf(RisorsaNonTrovataException.class);
        // ...oppure indicando lo spazio altrui
        assertThatThrownBy(() -> transazioneService.dettaglio(
                spazio.getId(), altroUtente.getId(), transazione.id()))
                .isInstanceOf(RisorsaNonTrovataException.class);
    }

    @Test
    void ilTipoDeveCoincidereConLaCategoria() {
        TransazioneRequest richiesta = richiesta(vendite, TipoTransazione.USCITA, "50.00", "Incoerente");

        assertThatThrownBy(() -> transazioneService.crea(spazio.getId(), proprietario.getId(), richiesta))
                .isInstanceOf(OperazioneNonValidaException.class);
    }

    private TransazioneResponse creaComeProprietario(Categoria categoria, TipoTransazione tipo,
            String importo, String descrizione) {
        return transazioneService.crea(spazio.getId(), proprietario.getId(),
                richiesta(categoria, tipo, importo, descrizione));
    }

    private TransazioneRequest richiesta(Categoria categoria, TipoTransazione tipo, String importo,
            String descrizione) {
        return new TransazioneRequest(new BigDecimal(importo), descrizione, DATA, tipo,
                MetodoPagamento.CARTA, categoria.getId(), null);
    }

    private RicercaTransazioniRequest filtro(TipoTransazione tipo, BigDecimal importoMin) {
        return new RicercaTransazioniRequest(null, null, null, tipo, null, importoMin, null, null,
                null, null, null, null);
    }

    private Transazione salvaPagamentoStipendio() {
        Dipendente dipendente = new Dipendente();
        dipendente.setSpazioLavoro(spazio);
        dipendente.setNome("Mario");
        dipendente.setCognome("Rossi");
        dipendente.setStipendioMensile(new BigDecimal("1500.00"));
        dipendenteRepository.save(dipendente);

        Transazione stipendio = new Transazione();
        stipendio.setImporto(new BigDecimal("1500.00"));
        stipendio.setDescrizione("Stipendio Mario Rossi");
        stipendio.setDataTransazione(DATA);
        stipendio.setTipo(TipoTransazione.USCITA);
        stipendio.setMetodoPagamento(MetodoPagamento.BONIFICO);
        stipendio.setSpazioLavoro(spazio);
        stipendio.setCategoria(stipendi);
        stipendio.setDipendente(dipendente);
        return transazioneRepository.save(stipendio);
    }

    private SpazioLavoro creaSpazio(String nome) {
        SpazioLavoro nuovo = new SpazioLavoro();
        nuovo.setNome(nome);
        nuovo.setTipo(TipoSpazio.AZIENDALE);
        return spazioLavoroRepository.save(nuovo);
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

    private void aggiungiMembro(SpazioLavoro dove, Utente utente, RuoloSpazio ruolo) {
        MembroSpazio appartenenza = new MembroSpazio();
        appartenenza.setSpazioLavoro(dove);
        appartenenza.setUtente(utente);
        appartenenza.setRuolo(ruolo);
        membroSpazioRepository.save(appartenenza);
    }
}