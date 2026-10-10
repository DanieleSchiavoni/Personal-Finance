// service/AccessoSpazioService.java
package org.elis.progettoesempio.service;

import org.elis.progettoesempio.entity.MembroSpazio;

/**
 * Controlla che un utente appartenga a uno spazio di lavoro e abbia il ruolo richiesto.
 * Restituisce l'appartenenza (MembroSpazio), da cui si ricavano ruolo e spazio.
 */
public interface AccessoSpazioService {

    /** Qualunque membro. Se non appartiene allo spazio: 404. */
    MembroSpazio verificaMembro(Long spazioLavoroId, Long utenteId);

    /** PROPRIETARIO, AMMINISTRATORE, MEMBRO. Altrimenti: 403. */
    MembroSpazio verificaPuoScrivere(Long spazioLavoroId, Long utenteId);

    /** PROPRIETARIO, AMMINISTRATORE. Altrimenti: 403. */
    MembroSpazio verificaPuoGestire(Long spazioLavoroId, Long utenteId);

    /** Solo PROPRIETARIO. Altrimenti: 403. */
    MembroSpazio verificaProprietario(Long spazioLavoroId, Long utenteId);
}