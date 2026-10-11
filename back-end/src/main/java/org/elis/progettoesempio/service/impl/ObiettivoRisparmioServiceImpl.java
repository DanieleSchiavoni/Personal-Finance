package org.elis.progettoesempio.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.elis.progettoesempio.dto.request.ObiettivoRisparmioRequest;
import org.elis.progettoesempio.dto.request.VersamentoRequest;
import org.elis.progettoesempio.dto.response.ObiettivoRisparmioResponse;
import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.ObiettivoRisparmio;
import org.elis.progettoesempio.entity.enumerazioni.StatoObiettivo;
import org.elis.progettoesempio.exception.OperazioneNonValidaException;
import org.elis.progettoesempio.exception.RisorsaNonTrovataException;
import org.elis.progettoesempio.mapper.ObiettivoRisparmioMapper;
import org.elis.progettoesempio.repository.MembroSpazioRepository;
import org.elis.progettoesempio.repository.ObiettivoRisparmioRepository;
import org.elis.progettoesempio.service.AccessoSpazioService;
import org.elis.progettoesempio.service.NotificaService;
import org.elis.progettoesempio.service.ObiettivoRisparmioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ObiettivoRisparmioServiceImpl implements ObiettivoRisparmioService {

    private static final BigDecimal CENTO = new BigDecimal("100.0");

    private final ObiettivoRisparmioRepository obiettivoRepository;
    private final MembroSpazioRepository membroSpazioRepository;
    private final AccessoSpazioService accessoSpazioService;
    private final NotificaService notificaService;
    private final ObiettivoRisparmioMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ObiettivoRisparmioResponse> elenca(Long spazioLavoroId, Long utenteId) {
        accessoSpazioService.verificaMembro(spazioLavoroId, utenteId);

        // Prima gli obiettivi con la scadenza più vicina; quelli senza scadenza in fondo
        return obiettivoRepository.findBySpazioLavoroIdOrderByScadenzaAsc(spazioLavoroId).stream()
                .sorted(Comparator
                        .comparing(ObiettivoRisparmio::getScadenza, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ObiettivoRisparmio::getNome))
                .map(this::costruisciRisposta)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ObiettivoRisparmioResponse dettaglio(Long spazioLavoroId, Long utenteId, Long obiettivoId) {
        accessoSpazioService.verificaMembro(spazioLavoroId, utenteId);
        return costruisciRisposta(trovaObiettivoDelloSpazio(obiettivoId, spazioLavoroId));
    }

    @Override
    @Transactional
    public ObiettivoRisparmioResponse crea(Long spazioLavoroId, Long utenteId,
            ObiettivoRisparmioRequest richiesta) {
        MembroSpazio membro = accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        verificaScadenzaNonPassata(richiesta.scadenza());

        ObiettivoRisparmio obiettivo = new ObiettivoRisparmio();
        obiettivo.setSpazioLavoro(membro.getSpazioLavoro());
        obiettivo.setCreatoDa(membro.getUtente());
        copiaDati(obiettivo, richiesta);
        obiettivo.setStato(calcolaStato(obiettivo));
        obiettivoRepository.save(obiettivo);

        return costruisciRisposta(obiettivo);
    }

    @Override
    @Transactional
    public ObiettivoRisparmioResponse aggiorna(Long spazioLavoroId, Long utenteId, Long obiettivoId,
            ObiettivoRisparmioRequest richiesta) {
        accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        ObiettivoRisparmio obiettivo = trovaObiettivoDelloSpazio(obiettivoId, spazioLavoroId);

        // Si controlla la scadenza solo se è stata cambiata: un obiettivo già scaduto resta modificabile
        if (!Objects.equals(obiettivo.getScadenza(), richiesta.scadenza())) {
            verificaScadenzaNonPassata(richiesta.scadenza());
        }
        copiaDati(obiettivo, richiesta);
        obiettivo.setStato(calcolaStato(obiettivo));

        return costruisciRisposta(obiettivo);
    }

    @Override
    @Transactional
    public void elimina(Long spazioLavoroId, Long utenteId, Long obiettivoId) {
        accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        obiettivoRepository.delete(trovaObiettivoDelloSpazio(obiettivoId, spazioLavoroId));
    }

    @Override
    @Transactional
    public ObiettivoRisparmioResponse aggiungiVersamento(Long spazioLavoroId, Long utenteId, Long obiettivoId,
            VersamentoRequest richiesta) {
        accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        ObiettivoRisparmio obiettivo = trovaObiettivoDelloSpazio(obiettivoId, spazioLavoroId);

        boolean giaCompletato = èRaggiunto(obiettivo);
        obiettivo.setImportoAttuale(obiettivo.getImportoAttuale().add(richiesta.importo()));
        obiettivo.setStato(calcolaStato(obiettivo));

        // Si notifica una volta sola, nel momento in cui l'obiettivo viene raggiunto
        if (!giaCompletato && èRaggiunto(obiettivo)) {
            notificaObiettivoRaggiunto(obiettivo);
        }
        return costruisciRisposta(obiettivo);
    }

    private void notificaObiettivoRaggiunto(ObiettivoRisparmio obiettivo) {
        String messaggio = String.format(Locale.ITALY,
                "L'obiettivo '%s' è stato raggiunto (%,.2f)",
                obiettivo.getNome(), obiettivo.getImportoObiettivo());

        membroSpazioRepository.trovaPerSpazioConUtente(obiettivo.getSpazioLavoro().getId()).stream()
                .filter(m -> m.getRuolo().puoGestire())
                .forEach(m -> notificaService.creaPerUtente(m.getUtente(), "Obiettivo raggiunto", messaggio));
    }

    private ObiettivoRisparmio trovaObiettivoDelloSpazio(Long obiettivoId, Long spazioLavoroId) {
        return obiettivoRepository.findByIdAndSpazioLavoroId(obiettivoId, spazioLavoroId)
                .orElseThrow(() -> new RisorsaNonTrovataException("Obiettivo di risparmio non trovato"));
    }

    private void copiaDati(ObiettivoRisparmio obiettivo, ObiettivoRisparmioRequest richiesta) {
        obiettivo.setNome(richiesta.nome().trim());
        obiettivo.setDescrizione(richiesta.descrizione());
        obiettivo.setImportoObiettivo(richiesta.importoObiettivo());
        obiettivo.setScadenza(richiesta.scadenza());
    }

    private void verificaScadenzaNonPassata(LocalDate scadenza) {
        if (scadenza != null && scadenza.isBefore(LocalDate.now())) {
            throw new OperazioneNonValidaException("La scadenza non può essere una data passata");
        }
    }

    private ObiettivoRisparmioResponse costruisciRisposta(ObiettivoRisparmio obiettivo) {
        BigDecimal importoMancante = obiettivo.getImportoObiettivo()
                .subtract(obiettivo.getImportoAttuale())
                .max(BigDecimal.ZERO);

        // Percentuale con una cifra decimale, troncata e mai oltre 100
        BigDecimal percentuale = obiettivo.getImportoAttuale().multiply(BigDecimal.valueOf(100))
                .divide(obiettivo.getImportoObiettivo(), 1, RoundingMode.DOWN)
                .min(CENTO);

        // Lo stato si calcola ad ogni lettura: non c'è nessun processo che lo aggiorni col passare dei giorni
        return mapper.aResponse(obiettivo, importoMancante, percentuale, calcolaStato(obiettivo));
    }

    private boolean èRaggiunto(ObiettivoRisparmio obiettivo) {
        return obiettivo.getImportoAttuale().compareTo(obiettivo.getImportoObiettivo()) >= 0;
    }

    private StatoObiettivo calcolaStato(ObiettivoRisparmio obiettivo) {
        if (èRaggiunto(obiettivo)) {
            return StatoObiettivo.COMPLETATO;
        }
        if (obiettivo.getScadenza() != null && obiettivo.getScadenza().isBefore(LocalDate.now())) {
            return StatoObiettivo.SCADUTO;
        }
        return StatoObiettivo.IN_CORSO;
    }
}