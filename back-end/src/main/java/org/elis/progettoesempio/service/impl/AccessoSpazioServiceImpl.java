// service/impl/AccessoSpazioServiceImpl.java
package org.elis.progettoesempio.service.impl;

import java.util.EnumSet;
import java.util.Set;

import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.exception.AccessoNegatoException;
import org.elis.progettoesempio.exception.RisorsaNonTrovataException;
import org.elis.progettoesempio.repository.MembroSpazioRepository;
import org.elis.progettoesempio.service.AccessoSpazioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccessoSpazioServiceImpl implements AccessoSpazioService {

    private static final Set<RuoloSpazio> RUOLI_SCRITTURA =
            EnumSet.of(RuoloSpazio.PROPRIETARIO, RuoloSpazio.AMMINISTRATORE, RuoloSpazio.MEMBRO);

    private static final Set<RuoloSpazio> RUOLI_GESTIONE =
            EnumSet.of(RuoloSpazio.PROPRIETARIO, RuoloSpazio.AMMINISTRATORE);

    private static final Set<RuoloSpazio> RUOLI_PROPRIETARIO =
            EnumSet.of(RuoloSpazio.PROPRIETARIO);

    private final MembroSpazioRepository membroSpazioRepository;

    @Override
    @Transactional(readOnly = true)
    public MembroSpazio verificaMembro(Long spazioLavoroId, Long utenteId) {
        // Messaggio generico: a chi non è membro non si rivela se lo spazio esiste
        return membroSpazioRepository.findBySpazioLavoroIdAndUtenteId(spazioLavoroId, utenteId)
                .orElseThrow(() -> new RisorsaNonTrovataException("Spazio di lavoro non trovato"));
    }

    @Override
    @Transactional(readOnly = true)
    public MembroSpazio verificaPuoScrivere(Long spazioLavoroId, Long utenteId) {
        return verificaRuolo(spazioLavoroId, utenteId, RUOLI_SCRITTURA);
    }

    @Override
    @Transactional(readOnly = true)
    public MembroSpazio verificaPuoGestire(Long spazioLavoroId, Long utenteId) {
        return verificaRuolo(spazioLavoroId, utenteId, RUOLI_GESTIONE);
    }

    @Override
    @Transactional(readOnly = true)
    public MembroSpazio verificaProprietario(Long spazioLavoroId, Long utenteId) {
        return verificaRuolo(spazioLavoroId, utenteId, RUOLI_PROPRIETARIO);
    }

    private MembroSpazio verificaRuolo(Long spazioLavoroId, Long utenteId, Set<RuoloSpazio> ruoliAmmessi) {
        MembroSpazio membro = verificaMembro(spazioLavoroId, utenteId);
        if (!ruoliAmmessi.contains(membro.getRuolo())) {
            throw new AccessoNegatoException("Non hai i permessi per eseguire questa operazione");
        }
        return membro;
    }
}