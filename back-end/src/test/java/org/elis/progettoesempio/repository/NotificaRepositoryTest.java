// repository/NotificaRepositoryTest.java
package org.elis.progettoesempio.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.elis.progettoesempio.entity.Notifica;
import org.elis.progettoesempio.entity.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class NotificaRepositoryTest {

    @Autowired
    private NotificaRepository notificaRepository;
    @Autowired
    private UtenteRepository utenteRepository;

    private Utente mario;
    private Utente luca;

    @BeforeEach
    void preparaDati() {
        mario = creaUtente("mario");
        luca = creaUtente("luca");
        creaNotifica(mario);
        creaNotifica(mario);
        creaNotifica(luca);
    }

    @Test
    void ilConteggioDelleNonLetteEPerUtente() {
        assertThat(notificaRepository.countByUtenteIdAndLettaFalse(mario.getId())).isEqualTo(2);
        assertThat(notificaRepository.countByUtenteIdAndLettaFalse(luca.getId())).isEqualTo(1);
    }

    @Test
    void segnareTutteComeLetteRiguardaSoloLUtenteIndicato() {
        int aggiornate = notificaRepository.segnaTutteComeLette(mario.getId());

        assertThat(aggiornate).isEqualTo(2);
        assertThat(notificaRepository.countByUtenteIdAndLettaFalse(mario.getId())).isZero();
        assertThat(notificaRepository.countByUtenteIdAndLettaFalse(luca.getId())).isEqualTo(1);
    }

    private void creaNotifica(Utente destinatario) {
        Notifica notifica = new Notifica();
        notifica.setUtente(destinatario);
        notifica.setTitolo("Titolo di prova");
        notifica.setMessaggio("Messaggio di prova");
        notificaRepository.save(notifica);
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
}