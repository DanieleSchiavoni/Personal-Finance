// service/UtenteService.java
package org.elis.progettoesempio.service;

import org.elis.progettoesempio.dto.request.AggiornaProfiloRequest;
import org.elis.progettoesempio.dto.response.UtenteResponse;

/** Operazioni dell'utente sul proprio account (il cambio password arriva con la Fase 6) */
public interface UtenteService {

    UtenteResponse profilo(Long utenteId);

    UtenteResponse aggiornaProfilo(Long utenteId, AggiornaProfiloRequest richiesta);

    /**
     * Elimina l'account. Gli spazi di cui sei l'unico membro vengono eliminati;
     * se sei proprietario di uno spazio condiviso l'operazione è bloccata (409).
     */
    void eliminaAccount(Long utenteId);
}