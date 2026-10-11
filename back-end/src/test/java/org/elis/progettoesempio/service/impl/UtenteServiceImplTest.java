// UtenteServiceImplTest.java
package org.elis.progettoesempio.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.elis.progettoesempio.dto.request.AggiornaProfiloRequest;
import org.elis.progettoesempio.dto.response.UtenteResponse;
import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Utente;

import org.elis.progettoesempio.entity.enumerazioni.RuoloUtente;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.exception.ConflittoException;
import org.elis.progettoesempio.exception.OperazioneNonValidaException;
import org.elis.progettoesempio.mapper.UtenteMapper;
import org.elis.progettoesempio.repository.MembroSpazioRepository;
import org.elis.progettoesempio.repository.SpazioLavoroRepository;
import org.elis.progettoesempio.repository.UtenteRepository;
import org.elis.progettoesempio.service.UtenteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({ UtenteServiceImpl.class, UtenteMapper.class })
class UtenteServiceImplTest {

    @Autowired
    private UtenteService utenteService;
    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private SpazioLavoroRepository spazioLavoroRepository;
    @Autowired
    private MembroSpazioRepository membroSpazioRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void ilProfiloSiAggiornaEL_emailVieneSalvataInMinuscolo() {
        Utente mario = creaUtente("mario", RuoloUtente.UTENTE);
        svuotaContesto();

        UtenteResponse aggiornato = utenteService.aggiornaProfilo(mario.getId(),
                new AggiornaProfiloRequest("mario.rossi", "Mario.Rossi@Test.it", "Mario", "Rossi"));

        assertThat(aggiornato.username()).isEqualTo("mario.rossi");
        assertThat(aggiornato.email()).isEqualTo("mario.rossi@test.it");
        assertThat(aggiornato.cognome()).isEqualTo("Rossi");
    }

    @Test
    void nonSiPuoUsareLEmailDiUnAltroUtente() {
        Utente mario = creaUtente("mario", RuoloUtente.UTENTE);
        creaUtente("luca", RuoloUtente.UTENTE); // luca@test.it
        svuotaContesto();

        assertThatThrownBy(() -> utenteService.aggiornaProfilo(mario.getId(),
                new AggiornaProfiloRequest("mario", "luca@test.it", "Mario", "Test")))
                .isInstanceOf(ConflittoException.class);
    }

    @Test
    void conSoloLoSpazioPersonaleLAccountSiEliminaConIlSuoSpazio() {
        Utente mario = creaUtente("mario", RuoloUtente.UTENTE);
        SpazioLavoro personale = creaSpazio(TipoSpazio.PERSONALE);
        aggiungiMembro(personale, mario, RuoloSpazio.PROPRIETARIO);
        svuotaContesto();

        utenteService.eliminaAccount(mario.getId());

        assertThat(utenteRepository.findById(mario.getId())).isEmpty();
        assertThat(spazioLavoroRepository.findById(personale.getId())).isEmpty();
    }

    @Test
    void conUnoSpazioCondivisoLEliminazioneEBloccata() {
        Utente mario = creaUtente("mario", RuoloUtente.UTENTE);
        Utente luca = creaUtente("luca", RuoloUtente.UTENTE);
        SpazioLavoro azienda = creaSpazio(TipoSpazio.AZIENDALE);
        aggiungiMembro(azienda, mario, RuoloSpazio.PROPRIETARIO);
        aggiungiMembro(azienda, luca, RuoloSpazio.MEMBRO);
        svuotaContesto();

        assertThatThrownBy(() -> utenteService.eliminaAccount(mario.getId()))
                .isInstanceOf(ConflittoException.class);
        assertThat(utenteRepository.findById(mario.getId())).isPresent();
    }

    @Test
    void ilMembroDiUnoSpazioAltruiPuoEliminarsiELoSpazioResta() {
        Utente proprietario = creaUtente("proprietario", RuoloUtente.UTENTE);
        Utente luca = creaUtente("luca", RuoloUtente.UTENTE);
        SpazioLavoro azienda = creaSpazio(TipoSpazio.AZIENDALE);
        aggiungiMembro(azienda, proprietario, RuoloSpazio.PROPRIETARIO);
        aggiungiMembro(azienda, luca, RuoloSpazio.MEMBRO);
        svuotaContesto();

        utenteService.eliminaAccount(luca.getId());

        assertThat(utenteRepository.findById(luca.getId())).isEmpty();
        assertThat(spazioLavoroRepository.findById(azienda.getId())).isPresent();
        assertThat(membroSpazioRepository.findBySpazioLavoroIdAndUtenteId(azienda.getId(), proprietario.getId()))
                .isPresent();
        assertThat(membroSpazioRepository.findBySpazioLavoroIdAndUtenteId(azienda.getId(), luca.getId()))
                .isEmpty();
    }

    @Test
    void lUltimoAmministratoreNonPuoEliminareIlProprioAccount() {
        Utente admin = creaUtente("admin", RuoloUtente.ADMIN);
        svuotaContesto();

        assertThatThrownBy(() -> utenteService.eliminaAccount(admin.getId()))
                .isInstanceOf(OperazioneNonValidaException.class);
    }

    /** Simula una richiesta vera, che parte sempre con il contesto di JPA vuoto */
    private void svuotaContesto() {
        entityManager.flush();
        entityManager.clear();
    }

    private Utente creaUtente(String nome, RuoloUtente ruolo) {
        Utente utente = new Utente();
        utente.setUsername(nome);
        utente.setEmail(nome + "@test.it");
        utente.setPassword("password-di-prova");
        utente.setNome(nome);
        utente.setCognome("Test");
        utente.setRuolo(ruolo);
        return utenteRepository.save(utente);
    }

    private SpazioLavoro creaSpazio(TipoSpazio tipo) {
        SpazioLavoro spazio = new SpazioLavoro();
        spazio.setNome("Spazio di prova");
        spazio.setTipo(tipo);
        return spazioLavoroRepository.save(spazio);
    }

    private void aggiungiMembro(SpazioLavoro spazio, Utente utente, RuoloSpazio ruolo) {
        MembroSpazio appartenenza = new MembroSpazio();
        appartenenza.setSpazioLavoro(spazio);
        appartenenza.setUtente(utente);
        appartenenza.setRuolo(ruolo);
        membroSpazioRepository.save(appartenenza);
    }
}