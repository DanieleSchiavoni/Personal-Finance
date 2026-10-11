// service/DipendenteService.java
package org.elis.progettoesempio.service;

import java.util.List;

import org.elis.progettoesempio.dto.request.DipendenteRequest;
import org.elis.progettoesempio.dto.request.PagamentoStipendioRequest;
import org.elis.progettoesempio.dto.response.DipendenteResponse;
import org.elis.progettoesempio.dto.response.PagamentoStipendioResponse;
import org.elis.progettoesempio.dto.response.TransazioneResponse;

/** Tutte le operazioni richiedono i permessi di gestione dello spazio (PROPRIETARIO o AMMINISTRATORE) */
public interface DipendenteService {

    List<DipendenteResponse> elenca(Long spazioLavoroId, Long utenteId);

    DipendenteResponse dettaglio(Long spazioLavoroId, Long utenteId, Long dipendenteId);

    DipendenteResponse crea(Long spazioLavoroId, Long utenteId, DipendenteRequest richiesta);

    DipendenteResponse aggiorna(Long spazioLavoroId, Long utenteId, Long dipendenteId, DipendenteRequest richiesta);

    /** Solo se il dipendente non ha pagamenti: altrimenti va disattivato */
    void elimina(Long spazioLavoroId, Long utenteId, Long dipendenteId);

    /** Registra lo stipendio di un mese: crea una transazione di uscita nella categoria "Stipendi" */
    TransazioneResponse registraPagamentoStipendio(Long spazioLavoroId, Long utenteId, Long dipendenteId,
            PagamentoStipendioRequest richiesta);

    /** Pagamenti già registrati, dal più recente */
    List<PagamentoStipendioResponse> elencaPagamenti(Long spazioLavoroId, Long utenteId, Long dipendenteId);
}