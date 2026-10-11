// ObiettivoRisparmioServiceImplTest.java
package org.elis.progettoesempio.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.elis.progettoesempio.dto.request.ObiettivoRisparmioRequest;
import org.elis.progettoesempio.dto.request.VersamentoRequest;
import org.elis.progettoesempio.dto.response.ObiettivoRisparmioResponse;
import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.ObiettivoRisparmio;
import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.entity.enumerazioni.StatoObiettivo;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.exception.AccessoNegatoException;
import org.elis.progettoesempio.exception.RisorsaNonTrovataException;
import org.elis.progettoesempio.mapper.NotificaMapper;
import org.elis.progettoesempio.mapper.ObiettivoRisparmioMapper;
import org.elis.progettoesempio.repository.MembroSpazioRepository;
import org.elis.progettoesempio.repository.NotificaRepository;
import org.elis.progettoesempio.repository.ObiettivoRisparmioRepository;
import org.elis.progettoesempio.repository.SpazioLavoroRepository;
import org.elis.progettoesempio.repository.UtenteRepository;
import org.elis.progettoesempio.service.ObiettivoRisparmioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({ ObiettivoRisparmioServiceImpl.class, AccessoSpazioServiceImpl.class, NotificaServiceImpl.class,
        ObiettivoRisparmioMapper.class, NotificaMapper.class })
class ObiettivoRisparmioServiceImplTest {

    @Autowired
    private ObiettivoRisparmioService obiettivoService;
    @Autowired
    private UtenteRepository utenteRepository;
    @Autowired
    private SpazioLavoroRepository spazioLavoroRepository;
    @Autowired
    private MembroSpazioRepository membroSpazioRepository;
    @Autowired
    private ObiettivoRisparmioRepository obiettivoRepository;
    @Autowired
    private NotificaRepository notificaRepository;

    private SpazioLavoro spazio;
    private SpazioLavoro altroSpazio;
    private Utente proprietario;
    private Utente membro;
    private Utente altroUtente;

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
    }

    @Test
    void ilVersamentoAggiornaPercentualeEImportoMancante() {
        ObiettivoRisparmioResponse creato = creaObiettivo("Nuova automobile", "20000.00");

        ObiettivoRisparmioResponse dopo = obiettivoService.aggiungiVersamento(
                spazio.getId(), proprietario.getId(), creato.id(), new VersamentoRequest(new BigDecimal("7500.00")));

        assertThat(dopo.importoAttuale()).isEqualByComparingTo("7500.00");
        assertThat(dopo.importoMancante()).isEqualByComparingTo("12500.00");
        assertThat(dopo.percentuale()).isEqualByComparingTo("37.5");
        assertThat(dopo.stato()).isEqualTo(StatoObiettivo.IN_CORSO);
    }

    @Test
    void raggiungereLObiettivoLoCompletaENotificaChiGestisce() {
        ObiettivoRisparmioResponse creato = creaObiettivo("Vacanza", "1000.00");

        ObiettivoRisparmioResponse dopo = obiettivoService.aggiungiVersamento(
                spazio.getId(), proprietario.getId(), creato.id(), new VersamentoRequest(new BigDecimal("1000.00")));

        assertThat(dopo.stato()).isEqualTo(StatoObiettivo.COMPLETATO);
        assertThat(dopo.percentuale()).isEqualByComparingTo("100.0");
        assertThat(notificaRepository.findByUtenteIdOrderByDataCreazioneDesc(proprietario.getId())).hasSize(1);
        assertThat(notificaRepository.countByUtenteIdAndLettaFalse(membro.getId())).isZero();
    }

    @Test
    void unObiettivoConScadenzaPassataEScaduto() {
        // Si salva direttamente con una scadenza passata e uno stato "vecchio" (IN_CORSO)
        ObiettivoRisparmio vecchio = new ObiettivoRisparmio();
        vecchio.setSpazioLavoro(spazio);
        vecchio.setNome("Obiettivo vecchio");
        vecchio.setImportoObiettivo(new BigDecimal("500.00"));
        vecchio.setImportoAttuale(new BigDecimal("100.00"));
        vecchio.setScadenza(LocalDate.now().minusDays(1));
        vecchio.setStato(StatoObiettivo.IN_CORSO);
        obiettivoRepository.save(vecchio);

        List<ObiettivoRisparmioResponse> obiettivi = obiettivoService.elenca(spazio.getId(), proprietario.getId());

        assertThat(obiettivi).hasSize(1);
        assertThat(obiettivi.get(0).stato()).isEqualTo(StatoObiettivo.SCADUTO);
    }

    @Test
    void ilMembroPuoLeggereMaNonScrivere() {
        ObiettivoRisparmioResponse creato = creaObiettivo("Vacanza", "1000.00");

        assertThat(obiettivoService.elenca(spazio.getId(), membro.getId())).hasSize(1);
        assertThatThrownBy(() -> obiettivoService.aggiungiVersamento(
                spazio.getId(), membro.getId(), creato.id(), new VersamentoRequest(new BigDecimal("10.00"))))
                .isInstanceOf(AccessoNegatoException.class);
        assertThatThrownBy(() -> obiettivoService.crea(spazio.getId(), membro.getId(),
                new ObiettivoRisparmioRequest("Altro", null, new BigDecimal("100.00"), null)))
                .isInstanceOf(AccessoNegatoException.class);
    }

    @Test
    void unObiettivoDiUnAltroSpazioNonSiTrova() {
        ObiettivoRisparmioResponse creato = creaObiettivo("Vacanza", "1000.00");

        assertThatThrownBy(() -> obiettivoService.dettaglio(altroSpazio.getId(), altroUtente.getId(), creato.id()))
                .isInstanceOf(RisorsaNonTrovataException.class);
        assertThatThrownBy(() -> obiettivoService.dettaglio(spazio.getId(), altroUtente.getId(), creato.id()))
                .isInstanceOf(RisorsaNonTrovataException.class);
    }

    private ObiettivoRisparmioResponse creaObiettivo(String nome, String importo) {
        return obiettivoService.crea(spazio.getId(), proprietario.getId(),
                new ObiettivoRisparmioRequest(nome, null, new BigDecimal(importo), LocalDate.now().plusYears(1)));
    }

    private SpazioLavoro creaSpazio(String nome) {
        SpazioLavoro nuovo = new SpazioLavoro();
        nuovo.setNome(nome);
        nuovo.setTipo(TipoSpazio.AZIENDALE);
        return spazioLavoroRepository.save(nuovo);
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