// service/impl/TransazioneServiceImpl.java
package org.elis.progettoesempio.service.impl;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Map;

import org.elis.progettoesempio.dto.request.RicercaTransazioniRequest;
import org.elis.progettoesempio.dto.request.TransazioneRequest;
import org.elis.progettoesempio.dto.response.PaginaResponse;
import org.elis.progettoesempio.dto.response.TransazioneResponse;
import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Transazione;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.exception.AccessoNegatoException;
import org.elis.progettoesempio.exception.OperazioneNonValidaException;
import org.elis.progettoesempio.exception.RisorsaNonTrovataException;
import org.elis.progettoesempio.mapper.TransazioneMapper;
import org.elis.progettoesempio.repository.CategoriaRepository;
import org.elis.progettoesempio.repository.TransazioneRepository;
import org.elis.progettoesempio.repository.specification.TransazioneSpecification;
import org.elis.progettoesempio.service.AccessoSpazioService;
import org.elis.progettoesempio.service.BudgetService;
import org.elis.progettoesempio.service.TransazioneService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransazioneServiceImpl implements TransazioneService {

    private static final int DIMENSIONE_PREDEFINITA = 20;

    /** Nome usato nell'API -> campo dell'entità. Solo questi campi sono ordinabili. */
    private static final Map<String, String> CAMPI_ORDINAMENTO = Map.of(
            "data", "dataTransazione",
            "importo", "importo",
            "descrizione", "descrizione",
            "categoria", "categoria.nome");

    private final TransazioneRepository transazioneRepository;
    private final CategoriaRepository categoriaRepository;
    private final AccessoSpazioService accessoSpazioService;
    private final BudgetService budgetService;
    private final TransazioneMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<TransazioneResponse> cerca(Long spazioLavoroId, Long utenteId,
            RicercaTransazioniRequest ricerca) {
        MembroSpazio membro = accessoSpazioService.verificaMembro(spazioLavoroId, utenteId);
        verificaIntervalli(ricerca);

        // MEMBRO e LETTORE non vedono i pagamenti di stipendio
        boolean nascondiStipendi = !membro.getRuolo().puoGestire();

        Page<Transazione> pagina = transazioneRepository.findAll(
                TransazioneSpecification.conFiltri(spazioLavoroId, ricerca, nascondiStipendi),
                costruisciPagina(ricerca));

        return PaginaResponse.da(pagina.map(mapper::aResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public TransazioneResponse dettaglio(Long spazioLavoroId, Long utenteId, Long transazioneId) {
        MembroSpazio membro = accessoSpazioService.verificaMembro(spazioLavoroId, utenteId);
        return mapper.aResponse(trovaVisibile(transazioneId, spazioLavoroId, membro));
    }

    @Override
    @Transactional
    public TransazioneResponse crea(Long spazioLavoroId, Long utenteId, TransazioneRequest richiesta) {
        MembroSpazio membro = accessoSpazioService.verificaPuoScrivere(spazioLavoroId, utenteId);
        SpazioLavoro spazio = membro.getSpazioLavoro();
        Categoria categoria = trovaCategoriaValida(spazioLavoroId, spazio.getTipo(), richiesta);

        Transazione transazione = new Transazione();
        transazione.setSpazioLavoro(spazio);
        transazione.setCreataDa(membro.getUtente());
        copiaDati(transazione, richiesta, categoria);
        transazioneRepository.save(transazione);

        if (transazione.getTipo() == TipoTransazione.USCITA) {
            budgetService.controllaSoglie(spazioLavoroId, categoria.getId(),
                    transazione.getDataTransazione(), transazione.getImporto());
        }
        return mapper.aResponse(transazione);
    }

    @Override
    @Transactional
    public TransazioneResponse aggiorna(Long spazioLavoroId, Long utenteId, Long transazioneId,
            TransazioneRequest richiesta) {
        MembroSpazio membro = accessoSpazioService.verificaPuoScrivere(spazioLavoroId, utenteId);
        Transazione transazione = trovaVisibile(transazioneId, spazioLavoroId, membro);
        verificaPuoModificare(transazione, membro, utenteId);

        Categoria categoria = trovaCategoriaValida(spazioLavoroId, membro.getSpazioLavoro().getTipo(), richiesta);

        // Quanto aumenta la spesa del budget interessato (serve per le notifiche)
        boolean stessoBudget = transazione.getCategoria().getId().equals(categoria.getId())
                && YearMonth.from(transazione.getDataTransazione()).equals(YearMonth.from(richiesta.data()));
        BigDecimal variazione = stessoBudget
                ? richiesta.importo().subtract(transazione.getImporto())
                : richiesta.importo();

        copiaDati(transazione, richiesta, categoria);

        if (transazione.getTipo() == TipoTransazione.USCITA) {
            budgetService.controllaSoglie(spazioLavoroId, categoria.getId(),
                    transazione.getDataTransazione(), variazione);
        }
        return mapper.aResponse(transazione);
    }

    @Override
    @Transactional
    public void elimina(Long spazioLavoroId, Long utenteId, Long transazioneId) {
        MembroSpazio membro = accessoSpazioService.verificaPuoScrivere(spazioLavoroId, utenteId);
        Transazione transazione = trovaVisibile(transazioneId, spazioLavoroId, membro);
        verificaPuoModificare(transazione, membro, utenteId);

        transazioneRepository.delete(transazione);
    }

    /**
     * Cerca per id E per spazio: una transazione di un altro spazio non si trova mai (404),
     * anche cambiando l'id nella richiesta. I pagamenti di stipendio sono invisibili a chi non gestisce.
     */
    private Transazione trovaVisibile(Long transazioneId, Long spazioLavoroId, MembroSpazio membro) {
        Transazione transazione = transazioneRepository.findByIdAndSpazioLavoroId(transazioneId, spazioLavoroId)
                .orElseThrow(() -> new RisorsaNonTrovataException("Transazione non trovata"));

        if (transazione.getDipendente() != null && !membro.getRuolo().puoGestire()) {
            throw new RisorsaNonTrovataException("Transazione non trovata");
        }
        return transazione;
    }

    /** Un MEMBRO modifica solo le transazioni che ha inserito lui; gli altri ruoli di scrittura tutte */
    private void verificaPuoModificare(Transazione transazione, MembroSpazio membro, Long utenteId) {
        if (membro.getRuolo() == RuoloSpazio.MEMBRO) {
            boolean èSua = transazione.getCreataDa() != null
                    && transazione.getCreataDa().getId().equals(utenteId);
            if (!èSua) {
                throw new AccessoNegatoException("Puoi modificare solo le transazioni inserite da te");
            }
        }
    }

    private Categoria trovaCategoriaValida(Long spazioLavoroId, TipoSpazio tipoSpazio, TransazioneRequest richiesta) {
        Categoria categoria = categoriaRepository
                .trovaVisibilePerId(richiesta.categoriaId(), spazioLavoroId, tipoSpazio)
                .orElseThrow(() -> new RisorsaNonTrovataException("Categoria non trovata"));

        if (categoria.getTipo() != richiesta.tipo()) {
            throw new OperazioneNonValidaException("Il tipo della transazione non coincide con quello della categoria");
        }
        return categoria;
    }

    private void copiaDati(Transazione transazione, TransazioneRequest richiesta, Categoria categoria) {
        transazione.setImporto(richiesta.importo());
        transazione.setDescrizione(richiesta.descrizione().trim());
        transazione.setDataTransazione(richiesta.data());
        transazione.setTipo(categoria.getTipo());
        transazione.setMetodoPagamento(richiesta.metodoPagamento());
        transazione.setNote(richiesta.note());
        transazione.setCategoria(categoria);
    }

    private void verificaIntervalli(RicercaTransazioniRequest ricerca) {
        if (ricerca.dal() != null && ricerca.al() != null && ricerca.dal().isAfter(ricerca.al())) {
            throw new OperazioneNonValidaException("La data iniziale non può essere successiva a quella finale");
        }
        if (ricerca.importoMin() != null && ricerca.importoMax() != null
                && ricerca.importoMin().compareTo(ricerca.importoMax()) > 0) {
            throw new OperazioneNonValidaException("L'importo minimo non può superare quello massimo");
        }
    }

    private Pageable costruisciPagina(RicercaTransazioniRequest ricerca) {
        int pagina = ricerca.pagina() != null ? ricerca.pagina() : 0;
        int dimensione = ricerca.dimensione() != null ? ricerca.dimensione() : DIMENSIONE_PREDEFINITA;

        String nomeApi = ricerca.ordinaPer() != null ? ricerca.ordinaPer().toLowerCase(Locale.ROOT) : "data";
        String campo = CAMPI_ORDINAMENTO.get(nomeApi);
        if (campo == null) {
            throw new OperazioneNonValidaException("Campo di ordinamento non valido");
        }
        Sort.Direction direzione = ricerca.direzione() != null
                ? Sort.Direction.fromString(ricerca.direzione())
                : Sort.Direction.DESC;

        // L'id come secondo criterio rende stabile l'ordine tra transazioni uguali
        return PageRequest.of(pagina, dimensione, Sort.by(direzione, campo).and(Sort.by(direzione, "id")));
    }
}