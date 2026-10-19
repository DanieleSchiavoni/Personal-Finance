// service/impl/ReportServiceImpl.java
package org.elis.progettoesempio.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.elis.progettoesempio.dto.response.BudgetResponse;
import org.elis.progettoesempio.dto.response.TransazioneResponse;
import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Transazione;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.exception.OperazioneNonValidaException;
import org.elis.progettoesempio.mapper.TransazioneMapper;
import org.elis.progettoesempio.repository.TransazioneRepository;
import org.elis.progettoesempio.service.AccessoSpazioService;
import org.elis.progettoesempio.service.BudgetService;
import org.elis.progettoesempio.service.ReportService;
import org.elis.progettoesempio.service.impl.DatiResoconto.RigaCategoria;
import org.elis.progettoesempio.service.impl.DatiResoconto.RigaStipendio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final TransazioneRepository transazioneRepository;
    private final AccessoSpazioService accessoSpazioService;
    private final BudgetService budgetService;
    private final TransazioneMapper transazioneMapper;
    private final GeneratorePdfResoconto generatore;

    @Override
    @Transactional(readOnly = true)
    public byte[] generaResocontoMensile(Long spazioLavoroId, Long utenteId, int mese, int anno) {
        MembroSpazio membro = accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        if (mese < 1 || mese > 12) {
            throw new OperazioneNonValidaException("Il mese deve essere compreso tra 1 e 12");
        }
        if (anno < 2000 || anno > 2100) {
            throw new OperazioneNonValidaException("Anno non valido");
        }

        YearMonth periodo = YearMonth.of(anno, mese);
        LocalDate dal = periodo.atDay(1);
        LocalDate al = periodo.atEndOfMonth();
        SpazioLavoro spazio = membro.getSpazioLavoro();

        BigDecimal entrate = transazioneRepository.sommaPerTipoNelPeriodo(spazioLavoroId, TipoTransazione.ENTRATA, dal, al);
        BigDecimal uscite = transazioneRepository.sommaPerTipoNelPeriodo(spazioLavoroId, TipoTransazione.USCITA, dal, al);
        List<BudgetResponse> budget = budgetService.elenca(spazioLavoroId, utenteId, mese, anno);

        List<Transazione> movimenti = transazioneRepository.trovaNelPeriodo(spazioLavoroId, dal, al);
        List<RigaStipendio> stipendi = movimenti.stream()
                .filter(movimento -> movimento.getDipendente() != null)
                .map(movimento -> new RigaStipendio(
                        movimento.getDipendente().getNome() + " " + movimento.getDipendente().getCognome(),
                        movimento.getImporto(),
                        movimento.getDataTransazione()))
                .toList();
        List<TransazioneResponse> elenco = movimenti.stream().map(transazioneMapper::aResponse).toList();

        DatiResoconto dati = new DatiResoconto(
                spazio.getNome(),
                spazio.getValuta(),
                periodo,
                LocalDateTime.now(),
                entrate,
                uscite,
                righeCategoria(spazioLavoroId, TipoTransazione.ENTRATA, dal, al),
                righeCategoria(spazioLavoroId, TipoTransazione.USCITA, dal, al),
                budget,
                stipendi,
                elenco);
        return generatore.genera(dati);
    }

    private List<RigaCategoria> righeCategoria(Long spazioLavoroId, TipoTransazione tipo, LocalDate dal, LocalDate al) {
        return transazioneRepository.sommaPerCategoriaNelPeriodo(spazioLavoroId, tipo, dal, al).stream()
                .map(totale -> new RigaCategoria(totale.getCategoriaNome(), totale.getTotale()))
                .toList();
    }
}