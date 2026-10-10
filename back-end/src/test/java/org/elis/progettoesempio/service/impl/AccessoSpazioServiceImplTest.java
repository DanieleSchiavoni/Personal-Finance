// back-end/src/test/java/org/elis/progettoesempio/service/impl/AccessoSpazioServiceImplTest.java
package org.elis.progettoesempio.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.entity.enumerazioni.RuoloSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.exception.AccessoNegatoException;
import org.elis.progettoesempio.exception.RisorsaNonTrovataException;
import org.elis.progettoesempio.repository.MembroSpazioRepository;
import org.elis.progettoesempio.repository.SpazioLavoroRepository;
import org.elis.progettoesempio.repository.UtenteRepository;
import org.elis.progettoesempio.service.AccessoSpazioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(AccessoSpazioServiceImpl.class)
class AccessoSpazioServiceImplTest {

    @Autowired
    private AccessoSpazioService accessoSpazioService;
    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private SpazioLavoroRepository spazioLavoroRepository;
    @Autowired
    private MembroSpazioRepository membroSpazioRepository;

    private SpazioLavoro spazio;
    private Utente proprietario;
    private Utente membro;
    private Utente lettore;
    private Utente estraneo;

    @BeforeEach
    void preparaDati() {
        spazio = new SpazioLavoro();
        spazio.setNome("Azienda di prova");
        spazio.setTipo(TipoSpazio.AZIENDALE);
        spazioLavoroRepository.save(spazio);

        proprietario = creaUtente("proprietario");
        membro = creaUtente("membro");
        lettore = creaUtente("lettore");
        estraneo = creaUtente("estraneo"); // non appartiene allo spazio

        aggiungiMembro(proprietario, RuoloSpazio.PROPRIETARIO);
        aggiungiMembro(membro, RuoloSpazio.MEMBRO);
        aggiungiMembro(lettore, RuoloSpazio.LETTORE);
    }

    @Test
    void unMembroPassaIlControlloDiAppartenenza() {
        assertThatCode(() -> accessoSpazioService.verificaMembro(spazio.getId(), lettore.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void unEstraneoNonVedeLoSpazio() {
        assertThatThrownBy(() -> accessoSpazioService.verificaMembro(spazio.getId(), estraneo.getId()))
                .isInstanceOf(RisorsaNonTrovataException.class);
    }

    @Test
    void ilLettoreNonPuoScrivere() {
        assertThatThrownBy(() -> accessoSpazioService.verificaPuoScrivere(spazio.getId(), lettore.getId()))
                .isInstanceOf(AccessoNegatoException.class);
    }

    @Test
    void ilMembroPuoScrivereMaNonGestire() {
        assertThatCode(() -> accessoSpazioService.verificaPuoScrivere(spazio.getId(), membro.getId()))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> accessoSpazioService.verificaPuoGestire(spazio.getId(), membro.getId()))
                .isInstanceOf(AccessoNegatoException.class);
    }

    @Test
    void soloIlProprietarioPassaIlControlloProprietario() {
        assertThatCode(() -> accessoSpazioService.verificaProprietario(spazio.getId(), proprietario.getId()))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> accessoSpazioService.verificaProprietario(spazio.getId(), membro.getId()))
                .isInstanceOf(AccessoNegatoException.class);
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