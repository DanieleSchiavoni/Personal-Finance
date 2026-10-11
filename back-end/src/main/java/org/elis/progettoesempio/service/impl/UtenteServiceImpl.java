// service/impl/UtenteServiceImpl.java
package org.elis.progettoesempio.service.impl;

import java.util.List;
import java.util.Locale;

import org.elis.progettoesempio.dto.request.AggiornaProfiloRequest;
import org.elis.progettoesempio.dto.response.UtenteResponse;
import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.Utente;

import org.elis.progettoesempio.entity.enumerazioni.RuoloUtente;
import org.elis.progettoesempio.exception.ConflittoException;
import org.elis.progettoesempio.exception.OperazioneNonValidaException;
import org.elis.progettoesempio.exception.RisorsaNonTrovataException;
import org.elis.progettoesempio.mapper.UtenteMapper;
import org.elis.progettoesempio.repository.BudgetRepository;
import org.elis.progettoesempio.repository.MembroSpazioRepository;
import org.elis.progettoesempio.repository.SpazioLavoroRepository;
import org.elis.progettoesempio.repository.TransazioneRepository;
import org.elis.progettoesempio.repository.UtenteRepository;
import org.elis.progettoesempio.service.UtenteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UtenteServiceImpl implements UtenteService {

    private final UtenteRepository utenteRepository;
    private final MembroSpazioRepository membroSpazioRepository;
    private final SpazioLavoroRepository spazioLavoroRepository;
    private final TransazioneRepository transazioneRepository;
    private final BudgetRepository budgetRepository;
    private final UtenteMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public UtenteResponse profilo(Long utenteId) {
        return mapper.aResponse(trovaUtente(utenteId));
    }

    @Override
    @Transactional
    public UtenteResponse aggiornaProfilo(Long utenteId, AggiornaProfiloRequest richiesta) {
        Utente utente = trovaUtente(utenteId);
        String username = richiesta.username().trim();
        String email = richiesta.email().trim().toLowerCase(Locale.ROOT);

        // Username ed email devono essere unici, ma ognuno può "riusare" i propri
        if (utenteRepository.findByUsername(username).filter(altro -> !altro.getId().equals(utenteId)).isPresent()) {
            throw new ConflittoException("Username già in uso");
        }
        if (utenteRepository.findByEmail(email).filter(altro -> !altro.getId().equals(utenteId)).isPresent()) {
            throw new ConflittoException("Email già in uso");
        }

        utente.setUsername(username);
        utente.setEmail(email);
        utente.setNome(richiesta.nome().trim());
        utente.setCognome(richiesta.cognome().trim());
        return mapper.aResponse(utente);
    }

    @Override
    @Transactional
    public void eliminaAccount(Long utenteId) {
        Utente utente = trovaUtente(utenteId);

        if (utente.getRuolo() == RuoloUtente.ADMIN && utenteRepository.countByRuolo(RuoloUtente.ADMIN) <= 1) {
            throw new OperazioneNonValidaException("L'ultimo amministratore non può eliminare il proprio account");
        }

        // Si memorizzano gli id: le operazioni di cancellazione di massa svuotano il contesto di JPA
        List<Long> spaziPosseduti = membroSpazioRepository
                .findByUtenteIdAndRuolo(utenteId, RuoloSpazio.PROPRIETARIO).stream()
                .map(m -> m.getSpazioLavoro().getId())
                .toList();

        // Prima si controlla tutto, poi si elimina: così non si cancella nulla a metà
        for (Long spazioId : spaziPosseduti) {
            if (membroSpazioRepository.trovaPerSpazioConUtente(spazioId).size() > 1) {
                throw new ConflittoException("Sei proprietario di uno spazio condiviso con altri membri: "
                        + "eliminalo o rimuovi gli altri membri prima di eliminare l'account");
            }
        }

        // Gli spazi di cui sei l'unico membro spariscono con i loro dati. Prima transazioni e budget
        // (le categorie in uso non si cancellano), il resto lo elimina il database a cascata.
        for (Long spazioId : spaziPosseduti) {
            transazioneRepository.eliminaTuttePerSpazioLavoro(spazioId);
            budgetRepository.eliminaTuttiPerSpazioLavoro(spazioId);
            spazioLavoroRepository.deleteById(spazioId);
        }

        // A cascata: appartenenze agli altri spazi, notifiche e refresh token.
        // Le transazioni inserite in spazi altrui restano, con autore vuoto.
        utenteRepository.deleteById(utenteId);
    }

    private Utente trovaUtente(Long utenteId) {
        return utenteRepository.findById(utenteId)
                .orElseThrow(() -> new RisorsaNonTrovataException("Utente non trovato"));
    }
}