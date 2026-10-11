// service/TransazioneService.java
package org.elis.progettoesempio.service;

import org.elis.progettoesempio.dto.request.RicercaTransazioniRequest;
import org.elis.progettoesempio.dto.request.TransazioneRequest;
import org.elis.progettoesempio.dto.response.PaginaResponse;
import org.elis.progettoesempio.dto.response.TransazioneResponse;

public interface TransazioneService {

    PaginaResponse<TransazioneResponse> cerca(Long spazioLavoroId, Long utenteId, RicercaTransazioniRequest ricerca);

    TransazioneResponse dettaglio(Long spazioLavoroId, Long utenteId, Long transazioneId);

    TransazioneResponse crea(Long spazioLavoroId, Long utenteId, TransazioneRequest richiesta);

    TransazioneResponse aggiorna(Long spazioLavoroId, Long utenteId, Long transazioneId, TransazioneRequest richiesta);

    void elimina(Long spazioLavoroId, Long utenteId, Long transazioneId);
}