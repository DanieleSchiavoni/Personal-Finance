// repository/TransazioneRepositoryTest.java
package org.elis.progettoesempio.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.Dipendente;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Transazione;
import org.elis.progettoesempio.entity.enumerazioni.MetodoPagamento;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class TransazioneRepositoryTest {

    private static final LocalDate INIZIO = LocalDate.of(2026, 10, 1);
    private static final LocalDate FINE = LocalDate.of(2026, 10, 31);

    @Autowired
    private TransazioneRepository transazioneRepository;
    @Autowired
    private SpazioLavoroRepository spazioLavoroRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private DipendenteRepository dipendenteRepository;

    private SpazioLavoro spazio;
    private SpazioLavoro altroSpazio;
    private Categoria alimentari;
    private Categoria casa;
    private Categoria stipendio;
    private Categoria categoriaAltroSpazio;

    @BeforeEach
    void preparaDati() {
        spazio = creaSpazio("Spazio A");
        altroSpazio = creaSpazio("Spazio B");
        alimentari = creaCategoria(spazio, "Alimentari", TipoTransazione.USCITA);
        casa = creaCategoria(spazio, "Casa", TipoTransazione.USCITA);
        stipendio = creaCategoria(spazio, "Stipendio", TipoTransazione.ENTRATA);
        categoriaAltroSpazio = creaCategoria(altroSpazio, "Alimentari", TipoTransazione.USCITA);
    }

    @Test
    void laSommaConsideraSoloPeriodoTipoESpazioGiusti() {
        salva(spazio, alimentari, TipoTransazione.USCITA, "100.00", LocalDate.of(2026, 10, 5));
        salva(spazio, alimentari, TipoTransazione.USCITA, "50.00", LocalDate.of(2026, 10, 20));
        salva(spazio, alimentari, TipoTransazione.USCITA, "10.00", INIZIO); // primo giorno: incluso
        salva(spazio, alimentari, TipoTransazione.USCITA, "5.00", FINE); // ultimo giorno: incluso
        salva(spazio, alimentari, TipoTransazione.USCITA, "70.00", LocalDate.of(2026, 9, 30)); // mese prima
        salva(spazio, alimentari, TipoTransazione.USCITA, "30.00", LocalDate.of(2026, 11, 1)); // mese dopo
        salva(spazio, stipendio, TipoTransazione.ENTRATA, "2000.00", LocalDate.of(2026, 10, 10));
        salva(altroSpazio, categoriaAltroSpazio, TipoTransazione.USCITA, "999.00", LocalDate.of(2026, 10, 5));

        assertThat(transazioneRepository.sommaPerTipoNelPeriodo(spazio.getId(), TipoTransazione.USCITA, INIZIO, FINE))
                .isEqualByComparingTo("165.00");
        assertThat(transazioneRepository.sommaPerTipoNelPeriodo(spazio.getId(), TipoTransazione.ENTRATA, INIZIO, FINE))
                .isEqualByComparingTo("2000.00");
        assertThat(transazioneRepository.sommaPerTipoNelPeriodo(altroSpazio.getId(), TipoTransazione.USCITA, INIZIO, FINE))
                .isEqualByComparingTo("999.00");
    }

    @Test
    void ITotaliPerCategoriaSonoOrdinatiDalPiuAlto() {
        salva(spazio, alimentari, TipoTransazione.USCITA, "80.00", LocalDate.of(2026, 10, 5));
        salva(spazio, alimentari, TipoTransazione.USCITA, "20.00", LocalDate.of(2026, 10, 12));
        salva(spazio, casa, TipoTransazione.USCITA, "600.00", LocalDate.of(2026, 10, 3));

        List<TotaleCategoria> totali = transazioneRepository
                .sommaPerCategoriaNelPeriodo(spazio.getId(), TipoTransazione.USCITA, INIZIO, FINE);

        assertThat(totali).hasSize(2);
        assertThat(totali.get(0).getCategoriaNome()).isEqualTo("Casa");
        assertThat(totali.get(0).getTotale()).isEqualByComparingTo("600.00");
        assertThat(totali.get(1).getCategoriaNome()).isEqualTo("Alimentari");
        assertThat(totali.get(1).getTotale()).isEqualByComparingTo("100.00");
    }

    @Test
    void ilSpesoDiUnaCategoriaIgnoraLeEntrate() {
        salva(spazio, stipendio, TipoTransazione.ENTRATA, "2000.00", LocalDate.of(2026, 10, 10));

        assertThat(transazioneRepository.sommaUsciteCategoriaNelPeriodo(spazio.getId(), stipendio.getId(), INIZIO, FINE))
                .isEqualByComparingTo("0");
    }

    @Test
    void unaTransazioneNonSiTrovaPerIdIndicandoUnAltroSpazio() {
        Transazione transazione = salva(spazio, alimentari, TipoTransazione.USCITA, "10.00", INIZIO);

        assertThat(transazioneRepository.findByIdAndSpazioLavoroId(transazione.getId(), spazio.getId())).isPresent();
        assertThat(transazioneRepository.findByIdAndSpazioLavoroId(transazione.getId(), altroSpazio.getId())).isEmpty();
    }

    @Test
    void siRiconosceUnPagamentoDiStipendioGiaRegistrato() {
        Dipendente dipendente = new Dipendente();
        dipendente.setSpazioLavoro(spazio);
        dipendente.setNome("Mario");
        dipendente.setCognome("Rossi");
        dipendente.setStipendioMensile(new BigDecimal("1500.00"));
        dipendenteRepository.save(dipendente);

        Transazione pagamento = salva(spazio, alimentari, TipoTransazione.USCITA, "1500.00", INIZIO);
        pagamento.setDipendente(dipendente);
        pagamento.setMeseStipendio(10);
        pagamento.setAnnoStipendio(2026);
        transazioneRepository.save(pagamento);

        assertThat(transazioneRepository.existsByDipendenteIdAndAnnoStipendioAndMeseStipendio(dipendente.getId(), 2026, 10))
                .isTrue();
        assertThat(transazioneRepository.existsByDipendenteIdAndAnnoStipendioAndMeseStipendio(dipendente.getId(), 2026, 11))
                .isFalse();
    }

    private Transazione salva(SpazioLavoro dove, Categoria categoria, TipoTransazione tipo, String importo,
            LocalDate data) {
        Transazione transazione = new Transazione();
        transazione.setImporto(new BigDecimal(importo));
        transazione.setDescrizione("Movimento di prova");
        transazione.setDataTransazione(data);
        transazione.setTipo(tipo);
        transazione.setMetodoPagamento(MetodoPagamento.CARTA);
        transazione.setSpazioLavoro(dove);
        transazione.setCategoria(categoria);
        return transazioneRepository.save(transazione);
    }

    private SpazioLavoro creaSpazio(String nome) {
        SpazioLavoro nuovo = new SpazioLavoro();
        nuovo.setNome(nome);
        nuovo.setTipo(TipoSpazio.AZIENDALE);
        return spazioLavoroRepository.save(nuovo);
    }

    private Categoria creaCategoria(SpazioLavoro dove, String nome, TipoTransazione tipo) {
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setTipo(tipo);
        categoria.setSpazioLavoro(dove);
        return categoriaRepository.save(categoria);
    }
}