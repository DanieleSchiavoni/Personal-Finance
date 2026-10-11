// service/impl/BudgetServiceImpl.java
package org.elis.progettoesempio.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.elis.progettoesempio.dto.request.AggiornaBudgetRequest;
import org.elis.progettoesempio.dto.request.BudgetRequest;
import org.elis.progettoesempio.dto.response.BudgetResponse;
import org.elis.progettoesempio.entity.Budget;
import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.enumerazioni.StatoBudget;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.exception.ConflittoException;
import org.elis.progettoesempio.exception.OperazioneNonValidaException;
import org.elis.progettoesempio.exception.RisorsaNonTrovataException;
import org.elis.progettoesempio.mapper.BudgetMapper;
import org.elis.progettoesempio.repository.BudgetRepository;
import org.elis.progettoesempio.repository.CategoriaRepository;
import org.elis.progettoesempio.repository.MembroSpazioRepository;
import org.elis.progettoesempio.repository.TotaleCategoria;
import org.elis.progettoesempio.repository.TransazioneRepository;
import org.elis.progettoesempio.service.AccessoSpazioService;
import org.elis.progettoesempio.service.BudgetService;
import org.elis.progettoesempio.service.NotificaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private static final BigDecimal CENTO = BigDecimal.valueOf(100);
    private static final BigDecimal SOGLIA_ATTENZIONE = BigDecimal.valueOf(80);

    private final BudgetRepository budgetRepository;
    private final CategoriaRepository categoriaRepository;
    private final TransazioneRepository transazioneRepository;
    private final MembroSpazioRepository membroSpazioRepository;
    private final AccessoSpazioService accessoSpazioService;
    private final NotificaService notificaService;
    private final BudgetMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> elenca(Long spazioLavoroId, Long utenteId, int mese, int anno) {
        accessoSpazioService.verificaMembro(spazioLavoroId, utenteId);
        YearMonth periodo = periodo(mese, anno);

        // Una sola query per ottenere il speso di tutte le categorie del mese
        Map<Long, BigDecimal> spesoPerCategoria = transazioneRepository
                .sommaPerCategoriaNelPeriodo(spazioLavoroId, TipoTransazione.USCITA,
                        periodo.atDay(1), periodo.atEndOfMonth())
                .stream()
                .collect(Collectors.toMap(TotaleCategoria::getCategoriaId, TotaleCategoria::getTotale));

        return budgetRepository.trovaPerPeriodo(spazioLavoroId, mese, anno).stream()
                .sorted(Comparator.comparing((Budget b) -> b.getCategoria().getNome()))
                .map(b -> costruisciRisposta(b,
                        spesoPerCategoria.getOrDefault(b.getCategoria().getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Override
    @Transactional
    public BudgetResponse crea(Long spazioLavoroId, Long utenteId, BudgetRequest richiesta) {
        MembroSpazio membro = accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        periodo(richiesta.mese(), richiesta.anno());

        Categoria categoria = categoriaRepository
                .trovaVisibilePerId(richiesta.categoriaId(), spazioLavoroId, membro.getSpazioLavoro().getTipo())
                .orElseThrow(() -> new RisorsaNonTrovataException("Categoria non trovata"));

        if (categoria.getTipo() != TipoTransazione.USCITA) {
            throw new OperazioneNonValidaException("Il budget si può impostare solo su categorie di uscita");
        }
        if (budgetRepository.existsBySpazioLavoroIdAndCategoriaIdAndMeseAndAnno(
                spazioLavoroId, categoria.getId(), richiesta.mese(), richiesta.anno())) {
            throw new ConflittoException("Esiste già un budget per questa categoria nel mese indicato");
        }

        Budget budget = new Budget();
        budget.setSpazioLavoro(membro.getSpazioLavoro());
        budget.setCategoria(categoria);
        budget.setImporto(richiesta.importo());
        budget.setMese(richiesta.mese());
        budget.setAnno(richiesta.anno());
        budgetRepository.save(budget);

        return costruisciRisposta(budget, spesoDelBudget(budget));
    }

    @Override
    @Transactional
    public BudgetResponse aggiorna(Long spazioLavoroId, Long utenteId, Long budgetId,
            AggiornaBudgetRequest richiesta) {
        accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        Budget budget = trovaBudgetDelloSpazio(budgetId, spazioLavoroId);

        budget.setImporto(richiesta.importo());
        return costruisciRisposta(budget, spesoDelBudget(budget));
    }

    @Override
    @Transactional
    public void elimina(Long spazioLavoroId, Long utenteId, Long budgetId) {
        accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        budgetRepository.delete(trovaBudgetDelloSpazio(budgetId, spazioLavoroId));
    }

    @Override
    @Transactional
    public void controllaSoglie(Long spazioLavoroId, Long categoriaId, LocalDate dataSpesa,
            BigDecimal variazione) {
        if (variazione.signum() <= 0) {
            return; // la spesa è diminuita: nessuna soglia può essere stata superata
        }
        budgetRepository
                .findBySpazioLavoroIdAndCategoriaIdAndMeseAndAnno(
                        spazioLavoroId, categoriaId, dataSpesa.getMonthValue(), dataSpesa.getYear())
                .ifPresent(budget -> notificaSeSogliaSuperata(budget, variazione));
    }

    private void notificaSeSogliaSuperata(Budget budget, BigDecimal variazione) {
        BigDecimal spesoDopo = spesoDelBudget(budget);
        StatoBudget statoDopo = calcolaStato(budget.getImporto(), spesoDopo);
        StatoBudget statoPrima = calcolaStato(budget.getImporto(), spesoDopo.subtract(variazione));

        // Si notifica solo se la spesa ha fatto peggiorare lo stato (es. da OK ad ATTENZIONE)
        if (statoDopo.ordinal() <= statoPrima.ordinal()) {
            return;
        }

        String titolo = statoDopo == StatoBudget.SUPERATO ? "Budget esaurito" : "Budget quasi esaurito";
        String messaggio = String.format(Locale.ITALY,
                "Il budget '%s' di %02d/%d ha raggiunto il %.1f%% (%,.2f su %,.2f)",
                budget.getCategoria().getNome(), budget.getMese(), budget.getAnno(),
                calcolaPercentuale(budget.getImporto(), spesoDopo), spesoDopo, budget.getImporto());

        // Le notifiche vanno a chi gestisce lo spazio
        membroSpazioRepository.trovaPerSpazioConUtente(budget.getSpazioLavoro().getId()).stream()
                .filter(m -> m.getRuolo().puoGestire())
                .forEach(m -> notificaService.creaPerUtente(m.getUtente(), titolo, messaggio));
    }

    private Budget trovaBudgetDelloSpazio(Long budgetId, Long spazioLavoroId) {
        return budgetRepository.findByIdAndSpazioLavoroId(budgetId, spazioLavoroId)
                .orElseThrow(() -> new RisorsaNonTrovataException("Budget non trovato"));
    }

    private BigDecimal spesoDelBudget(Budget budget) {
        YearMonth periodo = YearMonth.of(budget.getAnno(), budget.getMese());
        return transazioneRepository.sommaUsciteCategoriaNelPeriodo(
                budget.getSpazioLavoro().getId(), budget.getCategoria().getId(),
                periodo.atDay(1), periodo.atEndOfMonth());
    }

    private BudgetResponse costruisciRisposta(Budget budget, BigDecimal speso) {
        BigDecimal importo = budget.getImporto();
        return mapper.aResponse(
                budget,
                speso,
                importo.subtract(speso),
                calcolaPercentuale(importo, speso),
                calcolaStato(importo, speso));
    }

    /** Percentuale con una cifra decimale, troncata: arriva a 100 solo se il budget è davvero esaurito */
    private BigDecimal calcolaPercentuale(BigDecimal importo, BigDecimal speso) {
        return speso.multiply(CENTO).divide(importo, 1, RoundingMode.DOWN);
    }

    /** OK sotto l'80%, ATTENZIONE dall'80%, SUPERATO al 100% (calcolo sui valori esatti) */
    private StatoBudget calcolaStato(BigDecimal importo, BigDecimal speso) {
        if (speso.compareTo(importo) >= 0) {
            return StatoBudget.SUPERATO;
        }
        if (speso.multiply(CENTO).compareTo(importo.multiply(SOGLIA_ATTENZIONE)) >= 0) {
            return StatoBudget.ATTENZIONE;
        }
        return StatoBudget.OK;
    }

    private YearMonth periodo(int mese, int anno) {
        if (mese < 1 || mese > 12) {
            throw new OperazioneNonValidaException("Il mese deve essere compreso tra 1 e 12");
        }
        return YearMonth.of(anno, mese);
    }
}