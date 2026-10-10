// service/impl/SpazioLavoroServiceImpl.java
package org.elis.progettoesempio.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.elis.progettoesempio.dto.request.AggiornaSpazioLavoroRequest;
import org.elis.progettoesempio.dto.request.AggiungiMembroRequest;
import org.elis.progettoesempio.dto.request.CambiaRuoloMembroRequest;
import org.elis.progettoesempio.dto.request.CreaSpazioLavoroRequest;
import org.elis.progettoesempio.dto.response.MembroResponse;
import org.elis.progettoesempio.dto.response.SpazioLavoroResponse;
import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.entity.enumerazioni.RuoloSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.exception.AccessoNegatoException;
import org.elis.progettoesempio.exception.ConflittoException;
import org.elis.progettoesempio.exception.OperazioneNonValidaException;
import org.elis.progettoesempio.exception.RisorsaNonTrovataException;
import org.elis.progettoesempio.mapper.SpazioLavoroMapper;
import org.elis.progettoesempio.repository.BudgetRepository;
import org.elis.progettoesempio.repository.MembroSpazioRepository;
import org.elis.progettoesempio.repository.SpazioLavoroRepository;
import org.elis.progettoesempio.repository.TransazioneRepository;
import org.elis.progettoesempio.repository.UtenteRepository;
import org.elis.progettoesempio.service.AccessoSpazioService;
import org.elis.progettoesempio.service.SpazioLavoroService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpazioLavoroServiceImpl implements SpazioLavoroService {

    private final SpazioLavoroRepository spazioLavoroRepository;
    private final MembroSpazioRepository membroSpazioRepository;
    private final UtenteRepository utenteRepository;
    private final TransazioneRepository transazioneRepository;
    private final BudgetRepository budgetRepository;
    private final AccessoSpazioService accessoSpazioService;
    private final SpazioLavoroMapper mapper;

    @Override
    @Transactional
    public SpazioLavoroResponse creaSpazioPersonale(Utente utente) {
        SpazioLavoro spazio = new SpazioLavoro();
        spazio.setNome("Personale");
        spazio.setTipo(TipoSpazio.PERSONALE);
        spazioLavoroRepository.save(spazio);
        return mapper.aResponse(creaMembro(spazio, utente, RuoloSpazio.PROPRIETARIO));
    }

    @Override
    @Transactional
    public SpazioLavoroResponse creaSpazioAziendale(Long utenteId, CreaSpazioLavoroRequest richiesta) {
        Utente utente = utenteRepository.findById(utenteId)
                .orElseThrow(() -> new RisorsaNonTrovataException("Utente non trovato"));

        SpazioLavoro spazio = new SpazioLavoro();
        spazio.setNome(richiesta.nome().trim());
        spazio.setTipo(TipoSpazio.AZIENDALE);
        if (richiesta.valuta() != null) {
            spazio.setValuta(richiesta.valuta());
        }
        spazioLavoroRepository.save(spazio);

        // Chi crea lo spazio ne diventa il proprietario
        return mapper.aResponse(creaMembro(spazio, utente, RuoloSpazio.PROPRIETARIO));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpazioLavoroResponse> elencaSpaziUtente(Long utenteId) {
        return membroSpazioRepository.trovaPerUtenteConSpazio(utenteId).stream()
                .sorted(Comparator
                        .comparing((MembroSpazio m) -> m.getSpazioLavoro().getTipo())
                        .thenComparing(m -> m.getSpazioLavoro().getNome()))
                .map(mapper::aResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SpazioLavoroResponse dettaglio(Long spazioLavoroId, Long utenteId) {
        return mapper.aResponse(accessoSpazioService.verificaMembro(spazioLavoroId, utenteId));
    }

    @Override
    @Transactional
    public SpazioLavoroResponse aggiorna(Long spazioLavoroId, Long utenteId,
            AggiornaSpazioLavoroRequest richiesta) {
        MembroSpazio membro = accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);

        SpazioLavoro spazio = membro.getSpazioLavoro();
        spazio.setNome(richiesta.nome().trim());
        spazio.setValuta(richiesta.valuta());
        return mapper.aResponse(membro);
    }

    @Override
    @Transactional
    public void elimina(Long spazioLavoroId, Long utenteId) {
        MembroSpazio membro = accessoSpazioService.verificaProprietario(spazioLavoroId, utenteId);
        if (membro.getSpazioLavoro().getTipo() == TipoSpazio.PERSONALE) {
            throw new OperazioneNonValidaException("Lo spazio personale non può essere eliminato");
        }

        // Prima transazioni e budget, perché le categorie in uso non si possono cancellare (RESTRICT).
        // Il resto (membri, categorie, dipendenti, obiettivi) lo elimina il database a cascata.
        transazioneRepository.eliminaTuttePerSpazioLavoro(spazioLavoroId);
        budgetRepository.eliminaTuttiPerSpazioLavoro(spazioLavoroId);
        spazioLavoroRepository.deleteById(spazioLavoroId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembroResponse> elencaMembri(Long spazioLavoroId, Long utenteId) {
        accessoSpazioService.verificaMembro(spazioLavoroId, utenteId);

        return membroSpazioRepository.trovaPerSpazioConUtente(spazioLavoroId).stream()
                .sorted(Comparator
                        .comparing(MembroSpazio::getRuolo)
                        .thenComparing(m -> m.getUtente().getCognome()))
                .map(mapper::aMembroResponse)
                .toList();
    }

    @Override
    @Transactional
    public MembroResponse aggiungiMembro(Long spazioLavoroId, Long utenteId, AggiungiMembroRequest richiesta) {
        MembroSpazio chiamante = accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        SpazioLavoro spazio = chiamante.getSpazioLavoro();

        if (spazio.getTipo() == TipoSpazio.PERSONALE) {
            throw new OperazioneNonValidaException("Non si possono aggiungere membri a uno spazio personale");
        }
        if (richiesta.ruolo() == RuoloSpazio.PROPRIETARIO) {
            throw new OperazioneNonValidaException("Il ruolo PROPRIETARIO non può essere assegnato");
        }
        if (richiesta.ruolo() == RuoloSpazio.AMMINISTRATORE
                && chiamante.getRuolo() != RuoloSpazio.PROPRIETARIO) {
            throw new AccessoNegatoException("Solo il proprietario può nominare un amministratore");
        }

        Utente nuovoMembro = utenteRepository.findByEmail(richiesta.email().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RisorsaNonTrovataException("Nessun utente registrato con questa email"));

        if (membroSpazioRepository.findBySpazioLavoroIdAndUtenteId(spazioLavoroId, nuovoMembro.getId()).isPresent()) {
            throw new ConflittoException("L'utente fa già parte di questo spazio di lavoro");
        }

        return mapper.aMembroResponse(creaMembro(spazio, nuovoMembro, richiesta.ruolo()));
    }

    @Override
    @Transactional
    public MembroResponse cambiaRuoloMembro(Long spazioLavoroId, Long utenteId, Long membroUtenteId,
            CambiaRuoloMembroRequest richiesta) {
        accessoSpazioService.verificaProprietario(spazioLavoroId, utenteId);

        if (richiesta.ruolo() == RuoloSpazio.PROPRIETARIO) {
            throw new OperazioneNonValidaException("Il ruolo PROPRIETARIO non può essere assegnato");
        }
        MembroSpazio bersaglio = trovaMembro(spazioLavoroId, membroUtenteId);
        if (bersaglio.getRuolo() == RuoloSpazio.PROPRIETARIO) {
            throw new OperazioneNonValidaException("Il ruolo del proprietario non può essere modificato");
        }

        // L'entità è gestita da JPA: la modifica viene salvata al termine della transazione
        bersaglio.setRuolo(richiesta.ruolo());
        return mapper.aMembroResponse(bersaglio);
    }

    @Override
    @Transactional
    public void rimuoviMembro(Long spazioLavoroId, Long utenteId, Long membroUtenteId) {
        MembroSpazio chiamante = accessoSpazioService.verificaMembro(spazioLavoroId, utenteId);
        MembroSpazio bersaglio = trovaMembro(spazioLavoroId, membroUtenteId);

        if (bersaglio.getRuolo() == RuoloSpazio.PROPRIETARIO) {
            throw new OperazioneNonValidaException("Il proprietario non può essere rimosso");
        }

        // Ognuno può uscire da solo; per rimuovere altri servono i permessi di gestione
        boolean esceDaSolo = utenteId.equals(membroUtenteId);
        if (!esceDaSolo) {
            accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
            if (bersaglio.getRuolo() == RuoloSpazio.AMMINISTRATORE
                    && chiamante.getRuolo() != RuoloSpazio.PROPRIETARIO) {
                throw new AccessoNegatoException("Solo il proprietario può rimuovere un amministratore");
            }
        }

        membroSpazioRepository.delete(bersaglio);
    }

    private MembroSpazio trovaMembro(Long spazioLavoroId, Long utenteId) {
        return membroSpazioRepository.findBySpazioLavoroIdAndUtenteId(spazioLavoroId, utenteId)
                .orElseThrow(() -> new RisorsaNonTrovataException("Membro non trovato"));
    }

    private MembroSpazio creaMembro(SpazioLavoro spazio, Utente utente, RuoloSpazio ruolo) {
        MembroSpazio membro = new MembroSpazio();
        membro.setSpazioLavoro(spazio);
        membro.setUtente(utente);
        membro.setRuolo(ruolo);
        return membroSpazioRepository.save(membro);
    }
}