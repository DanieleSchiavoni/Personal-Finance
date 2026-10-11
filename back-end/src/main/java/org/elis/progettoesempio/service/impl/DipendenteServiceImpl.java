// service/impl/DipendenteServiceImpl.java
package org.elis.progettoesempio.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.elis.progettoesempio.dto.request.DipendenteRequest;
import org.elis.progettoesempio.dto.request.PagamentoStipendioRequest;
import org.elis.progettoesempio.dto.response.DipendenteResponse;
import org.elis.progettoesempio.dto.response.PagamentoStipendioResponse;
import org.elis.progettoesempio.dto.response.TransazioneResponse;
import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.Dipendente;
import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.Transazione;
import org.elis.progettoesempio.entity.enumerazioni.MetodoPagamento;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.exception.ConflittoException;
import org.elis.progettoesempio.exception.OperazioneNonValidaException;
import org.elis.progettoesempio.exception.RisorsaNonTrovataException;
import org.elis.progettoesempio.mapper.DipendenteMapper;
import org.elis.progettoesempio.mapper.TransazioneMapper;
import org.elis.progettoesempio.repository.CategoriaRepository;
import org.elis.progettoesempio.repository.DipendenteRepository;
import org.elis.progettoesempio.repository.TransazioneRepository;
import org.elis.progettoesempio.service.AccessoSpazioService;
import org.elis.progettoesempio.service.BudgetService;
import org.elis.progettoesempio.service.DipendenteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DipendenteServiceImpl implements DipendenteService {

    /** Categoria globale creata dalla migrazione Flyway */
    private static final String NOME_CATEGORIA_STIPENDI = "Stipendi";

    private final DipendenteRepository dipendenteRepository;
    private final CategoriaRepository categoriaRepository;
    private final TransazioneRepository transazioneRepository;
    private final AccessoSpazioService accessoSpazioService;
    private final BudgetService budgetService;
    private final DipendenteMapper mapper;
    private final TransazioneMapper transazioneMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DipendenteResponse> elenca(Long spazioLavoroId, Long utenteId) {
        accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);

        return dipendenteRepository.findBySpazioLavoroIdOrderByCognomeAscNomeAsc(spazioLavoroId).stream()
                .map(mapper::aResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DipendenteResponse dettaglio(Long spazioLavoroId, Long utenteId, Long dipendenteId) {
        accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        return mapper.aResponse(trovaDipendenteDelloSpazio(dipendenteId, spazioLavoroId));
    }

    @Override
    @Transactional
    public DipendenteResponse crea(Long spazioLavoroId, Long utenteId, DipendenteRequest richiesta) {
        MembroSpazio membro = accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        if (membro.getSpazioLavoro().getTipo() != TipoSpazio.AZIENDALE) {
            throw new OperazioneNonValidaException("Solo gli spazi aziendali possono avere dipendenti");
        }

        Dipendente dipendente = new Dipendente();
        dipendente.setSpazioLavoro(membro.getSpazioLavoro());
        copiaDati(dipendente, richiesta);
        dipendente.setAttivo(richiesta.attivo() == null || richiesta.attivo());
        dipendenteRepository.save(dipendente);

        return mapper.aResponse(dipendente);
    }

    @Override
    @Transactional
    public DipendenteResponse aggiorna(Long spazioLavoroId, Long utenteId, Long dipendenteId,
            DipendenteRequest richiesta) {
        accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        Dipendente dipendente = trovaDipendenteDelloSpazio(dipendenteId, spazioLavoroId);

        // I pagamenti già registrati conservano il loro importo: cambiare lo stipendio non li modifica
        copiaDati(dipendente, richiesta);
        if (richiesta.attivo() != null) {
            dipendente.setAttivo(richiesta.attivo());
        }
        return mapper.aResponse(dipendente);
    }

    @Override
    @Transactional
    public void elimina(Long spazioLavoroId, Long utenteId, Long dipendenteId) {
        accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        Dipendente dipendente = trovaDipendenteDelloSpazio(dipendenteId, spazioLavoroId);

        // Eliminandolo, i suoi stipendi perderebbero il collegamento e diventerebbero visibili a tutti i membri
        if (transazioneRepository.existsByDipendenteId(dipendenteId)) {
            throw new ConflittoException("Il dipendente ha pagamenti registrati: disattivalo invece di eliminarlo");
        }
        dipendenteRepository.delete(dipendente);
    }

    @Override
    @Transactional
    public TransazioneResponse registraPagamentoStipendio(Long spazioLavoroId, Long utenteId, Long dipendenteId,
            PagamentoStipendioRequest richiesta) {
        MembroSpazio membro = accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        Dipendente dipendente = trovaDipendenteDelloSpazio(dipendenteId, spazioLavoroId);

        if (!dipendente.isAttivo()) {
            throw new OperazioneNonValidaException("Il dipendente non è attivo");
        }
        if (transazioneRepository.existsByDipendenteIdAndAnnoStipendioAndMeseStipendio(
                dipendenteId, richiesta.anno(), richiesta.mese())) {
            throw new ConflittoException("Lo stipendio di questo mese è già stato registrato");
        }

        Categoria categoriaStipendi = categoriaRepository
                .findBySpazioLavoroIsNullAndNomeAndTipo(NOME_CATEGORIA_STIPENDI, TipoTransazione.USCITA)
                .orElseThrow(() -> new IllegalStateException("Manca la categoria di sistema 'Stipendi'"));

        Transazione pagamento = new Transazione();
        pagamento.setSpazioLavoro(membro.getSpazioLavoro());
        pagamento.setCreataDa(membro.getUtente());
        pagamento.setCategoria(categoriaStipendi);
        pagamento.setTipo(TipoTransazione.USCITA);
        pagamento.setDipendente(dipendente);
        pagamento.setMeseStipendio(richiesta.mese());
        pagamento.setAnnoStipendio(richiesta.anno());
        pagamento.setDescrizione(String.format("Stipendio %s %s %02d/%d",
                dipendente.getNome(), dipendente.getCognome(), richiesta.mese(), richiesta.anno()));

        // I campi facoltativi, se mancano, hanno un valore predefinito
        pagamento.setImporto(richiesta.importo() != null ? richiesta.importo() : dipendente.getStipendioMensile());
        pagamento.setDataTransazione(richiesta.dataPagamento() != null ? richiesta.dataPagamento() : LocalDate.now());
        pagamento.setMetodoPagamento(
                richiesta.metodoPagamento() != null ? richiesta.metodoPagamento() : MetodoPagamento.BONIFICO);
        transazioneRepository.save(pagamento);

        // Gli stipendi sono una spesa come le altre: possono far scattare un budget
        budgetService.controllaSoglie(spazioLavoroId, categoriaStipendi.getId(),
                pagamento.getDataTransazione(), pagamento.getImporto());

        return transazioneMapper.aResponse(pagamento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagamentoStipendioResponse> elencaPagamenti(Long spazioLavoroId, Long utenteId, Long dipendenteId) {
        accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        // Prima si verifica che il dipendente appartenga allo spazio, poi si leggono i suoi pagamenti
        trovaDipendenteDelloSpazio(dipendenteId, spazioLavoroId);

        return transazioneRepository.findByDipendenteIdOrderByAnnoStipendioDescMeseStipendioDesc(dipendenteId)
                .stream()
                .map(mapper::aPagamentoResponse)
                .toList();
    }

    private Dipendente trovaDipendenteDelloSpazio(Long dipendenteId, Long spazioLavoroId) {
        return dipendenteRepository.findByIdAndSpazioLavoroId(dipendenteId, spazioLavoroId)
                .orElseThrow(() -> new RisorsaNonTrovataException("Dipendente non trovato"));
    }

    private void copiaDati(Dipendente dipendente, DipendenteRequest richiesta) {
        dipendente.setNome(richiesta.nome().trim());
        dipendente.setCognome(richiesta.cognome().trim());
        dipendente.setMansione(richiesta.mansione());
        dipendente.setStipendioMensile(richiesta.stipendioMensile());
        dipendente.setDataAssunzione(richiesta.dataAssunzione());
    }
}