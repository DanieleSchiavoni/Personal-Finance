// service/impl/DashboardServiceImpl.java
package org.elis.progettoesempio.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.elis.progettoesempio.dto.request.RicercaTransazioniRequest;
import org.elis.progettoesempio.dto.response.BudgetResponse;
import org.elis.progettoesempio.dto.response.PuntoTrendResponse;
import org.elis.progettoesempio.dto.response.RiepilogoDashboardResponse;
import org.elis.progettoesempio.dto.response.RiepilogoDashboardResponse.SintesiBudget;
import org.elis.progettoesempio.dto.response.RiepilogoDashboardResponse.SintesiObiettivi;
import org.elis.progettoesempio.dto.response.TotaleCategoriaResponse;
import org.elis.progettoesempio.dto.response.TransazioneResponse;
import org.elis.progettoesempio.entity.ObiettivoRisparmio;
import org.elis.progettoesempio.entity.Transazione;
import org.elis.progettoesempio.entity.enumerazioni.Granularita;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.exception.OperazioneNonValidaException;
import org.elis.progettoesempio.mapper.DashboardMapper;
import org.elis.progettoesempio.repository.ObiettivoRisparmioRepository;
import org.elis.progettoesempio.repository.TotaleCategoria;
import org.elis.progettoesempio.repository.TransazioneRepository;
import org.elis.progettoesempio.service.AccessoSpazioService;
import org.elis.progettoesempio.service.BudgetService;
import org.elis.progettoesempio.service.DashboardService;
import org.elis.progettoesempio.service.TransazioneService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int MAX_PERIODI = 120;
    private static final int NUMERO_CATEGORIE_PRINCIPALI = 5;
    private static final int NUMERO_ULTIME_TRANSAZIONI = 5;
    private static final LocalDate DATA_MINIMA = LocalDate.of(1900, 1, 1);

    private final TransazioneRepository transazioneRepository;
    private final ObiettivoRisparmioRepository obiettivoRepository;
    private final AccessoSpazioService accessoSpazioService;
    private final BudgetService budgetService;
    private final TransazioneService transazioneService;
    private final DashboardMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public RiepilogoDashboardResponse riepilogo(Long spazioLavoroId, Long utenteId, int mese, int anno) {
        accessoSpazioService.verificaMembro(spazioLavoroId, utenteId);
        if (mese < 1 || mese > 12) {
            throw new OperazioneNonValidaException("Il mese deve essere compreso tra 1 e 12");
        }
        YearMonth periodo = YearMonth.of(anno, mese);
        LocalDate dal = periodo.atDay(1);
        LocalDate al = periodo.atEndOfMonth();

        BigDecimal saldoTotale = transazioneRepository.sommaPerTipo(spazioLavoroId, TipoTransazione.ENTRATA)
                .subtract(transazioneRepository.sommaPerTipo(spazioLavoroId, TipoTransazione.USCITA));
        BigDecimal entrateMese = transazioneRepository
                .sommaPerTipoNelPeriodo(spazioLavoroId, TipoTransazione.ENTRATA, dal, al);
        BigDecimal usciteMese = transazioneRepository
                .sommaPerTipoNelPeriodo(spazioLavoroId, TipoTransazione.USCITA, dal, al);

        return new RiepilogoDashboardResponse(
                mese,
                anno,
                saldoTotale,
                entrateMese,
                usciteMese,
                entrateMese.subtract(usciteMese),
                transazioneRepository.countBySpazioLavoroIdAndDataTransazioneBetween(spazioLavoroId, dal, al),
                sintesiBudget(spazioLavoroId, utenteId, mese, anno),
                sintesiObiettivi(spazioLavoroId),
                calcolaTotaliPerCategoria(spazioLavoroId, dal, al).stream()
                        .limit(NUMERO_CATEGORIE_PRINCIPALI)
                        .toList(),
                ultimeTransazioni(spazioLavoroId, utenteId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PuntoTrendResponse> trend(Long spazioLavoroId, Long utenteId, Granularita granularita,
            LocalDate dal, LocalDate al) {
        accessoSpazioService.verificaMembro(spazioLavoroId, utenteId);

        Granularita ampiezza = granularita != null ? granularita : Granularita.MESE;
        LocalDate oggi = LocalDate.now();
        LocalDate richiestoDal = dal != null ? dal : inizioPredefinito(ampiezza, oggi);
        LocalDate richiestoAl = al != null ? al : oggi;
        if (richiestoDal.isAfter(richiestoAl)) {
            throw new OperazioneNonValidaException("La data iniziale non può essere successiva a quella finale");
        }

        // Si lavora su periodi interi: dall'inizio del primo alla fine dell'ultimo
        LocalDate primoGiorno = inizioPeriodo(ampiezza, richiestoDal);
        LocalDate ultimoGiorno = finePeriodo(ampiezza, inizioPeriodo(ampiezza, richiestoAl));

        // Una "casella" per ogni periodo, anche se vuoto, così il grafico non ha buchi
        Map<LocalDate, Totali> caselle = new TreeMap<>();
        for (LocalDate inizio = primoGiorno; !inizio.isAfter(ultimoGiorno); inizio = periodoSuccessivo(ampiezza, inizio)) {
            if (caselle.size() >= MAX_PERIODI) {
                throw new OperazioneNonValidaException(
                        "Intervallo troppo ampio: al massimo " + MAX_PERIODI + " periodi per richiesta");
            }
            caselle.put(inizio, new Totali());
        }

        for (Transazione transazione : transazioneRepository.trovaNelPeriodo(spazioLavoroId, primoGiorno, ultimoGiorno)) {
            Totali totali = caselle.get(inizioPeriodo(ampiezza, transazione.getDataTransazione()));
            if (transazione.getTipo() == TipoTransazione.ENTRATA) {
                totali.entrate = totali.entrate.add(transazione.getImporto());
            } else {
                totali.uscite = totali.uscite.add(transazione.getImporto());
            }
        }

        // Il saldo progressivo parte da quanto accumulato prima del primo periodo
        LocalDate giornoPrecedente = primoGiorno.minusDays(1);
        BigDecimal saldoProgressivo = transazioneRepository
                .sommaPerTipoNelPeriodo(spazioLavoroId, TipoTransazione.ENTRATA, DATA_MINIMA, giornoPrecedente)
                .subtract(transazioneRepository
                        .sommaPerTipoNelPeriodo(spazioLavoroId, TipoTransazione.USCITA, DATA_MINIMA, giornoPrecedente));

        List<PuntoTrendResponse> punti = new ArrayList<>();
        for (Map.Entry<LocalDate, Totali> casella : caselle.entrySet()) {
            LocalDate inizio = casella.getKey();
            Totali totali = casella.getValue();
            BigDecimal saldoPeriodo = totali.entrate.subtract(totali.uscite);
            saldoProgressivo = saldoProgressivo.add(saldoPeriodo);

            punti.add(new PuntoTrendResponse(
                    etichetta(ampiezza, inizio),
                    inizio,
                    finePeriodo(ampiezza, inizio),
                    totali.entrate,
                    totali.uscite,
                    saldoPeriodo,
                    saldoProgressivo));
        }
        return punti;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TotaleCategoriaResponse> spesePerCategoria(Long spazioLavoroId, Long utenteId,
            LocalDate dal, LocalDate al) {
        accessoSpazioService.verificaMembro(spazioLavoroId, utenteId);

        YearMonth meseCorrente = YearMonth.now();
        LocalDate richiestoDal = dal != null ? dal : meseCorrente.atDay(1);
        LocalDate richiestoAl = al != null ? al : meseCorrente.atEndOfMonth();
        if (richiestoDal.isAfter(richiestoAl)) {
            throw new OperazioneNonValidaException("La data iniziale non può essere successiva a quella finale");
        }
        return calcolaTotaliPerCategoria(spazioLavoroId, richiestoDal, richiestoAl);
    }

    private List<TotaleCategoriaResponse> calcolaTotaliPerCategoria(Long spazioLavoroId, LocalDate dal, LocalDate al) {
        List<TotaleCategoria> totali = transazioneRepository
                .sommaPerCategoriaNelPeriodo(spazioLavoroId, TipoTransazione.USCITA, dal, al);
        BigDecimal sommaUscite = totali.stream()
                .map(TotaleCategoria::getTotale)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Percentuale arrotondata (non troncata): serve al grafico a torta
        return totali.stream()
                .map(t -> mapper.aTotaleCategoriaResponse(t,
                        percentuale(t.getTotale(), sommaUscite, RoundingMode.HALF_UP)))
                .toList();
    }

    private SintesiBudget sintesiBudget(Long spazioLavoroId, Long utenteId, int mese, int anno) {
        List<BudgetResponse> budget = budgetService.elenca(spazioLavoroId, utenteId, mese, anno);
        BigDecimal totale = budget.stream().map(BudgetResponse::importo).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal speso = budget.stream().map(BudgetResponse::speso).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SintesiBudget(totale, speso, percentuale(speso, totale, RoundingMode.DOWN));
    }

    private SintesiObiettivi sintesiObiettivi(Long spazioLavoroId) {
        List<ObiettivoRisparmio> obiettivi = obiettivoRepository.findBySpazioLavoroIdOrderByScadenzaAsc(spazioLavoroId);

        int completati = (int) obiettivi.stream()
                .filter(o -> o.getImportoAttuale().compareTo(o.getImportoObiettivo()) >= 0)
                .count();
        BigDecimal risparmiato = obiettivi.stream()
                .map(ObiettivoRisparmio::getImportoAttuale)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totaleObiettivi = obiettivi.stream()
                .map(ObiettivoRisparmio::getImportoObiettivo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SintesiObiettivi(obiettivi.size(), completati, risparmiato, totaleObiettivi);
    }

    /** Passa dal TransazioneService, così per MEMBRO e LETTORE gli stipendi restano nascosti */
    private List<TransazioneResponse> ultimeTransazioni(Long spazioLavoroId, Long utenteId) {
        RicercaTransazioniRequest ricerca = new RicercaTransazioniRequest(
                null, null, null, null, null, null, null, null,
                0, NUMERO_ULTIME_TRANSAZIONI, "data", "desc");
        return transazioneService.cerca(spazioLavoroId, utenteId, ricerca).contenuto();
    }

    private BigDecimal percentuale(BigDecimal parte, BigDecimal totale, RoundingMode arrotondamento) {
        if (totale.signum() == 0) {
            return new BigDecimal("0.0");
        }
        return parte.multiply(BigDecimal.valueOf(100)).divide(totale, 1, arrotondamento);
    }

    private LocalDate inizioPredefinito(Granularita ampiezza, LocalDate oggi) {
        return ampiezza == Granularita.MESE ? oggi.minusMonths(11) : oggi.minusWeeks(11);
    }

    /** Primo giorno del periodo che contiene la data: il primo del mese o il lunedì della settimana */
    private LocalDate inizioPeriodo(Granularita ampiezza, LocalDate data) {
        return ampiezza == Granularita.MESE
                ? data.withDayOfMonth(1)
                : data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate finePeriodo(Granularita ampiezza, LocalDate inizio) {
        return ampiezza == Granularita.MESE
                ? inizio.with(TemporalAdjusters.lastDayOfMonth())
                : inizio.plusDays(6);
    }

    private LocalDate periodoSuccessivo(Granularita ampiezza, LocalDate inizio) {
        return ampiezza == Granularita.MESE ? inizio.plusMonths(1) : inizio.plusWeeks(1);
    }

    private String etichetta(Granularita ampiezza, LocalDate inizio) {
        if (ampiezza == Granularita.MESE) {
            return String.format("%d-%02d", inizio.getYear(), inizio.getMonthValue());
        }
        // Settimana secondo lo standard ISO, ad esempio 2026-S41
        return String.format("%d-S%02d",
                inizio.get(IsoFields.WEEK_BASED_YEAR), inizio.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    /** Accumulatore dei totali di un periodo */
    private static final class Totali {
        private BigDecimal entrate = BigDecimal.ZERO;
        private BigDecimal uscite = BigDecimal.ZERO;
    }
}