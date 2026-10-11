// service/ObiettivoRisparmioService.java
package org.elis.progettoesempio.service;

import java.util.List;

import org.elis.progettoesempio.dto.request.ObiettivoRisparmioRequest;
import org.elis.progettoesempio.dto.request.VersamentoRequest;
import org.elis.progettoesempio.dto.response.ObiettivoRisparmioResponse;

public interface ObiettivoRisparmioService {

    List<ObiettivoRisparmioResponse> elenca(Long spazioLavoroId, Long utenteId);

    ObiettivoRisparmioResponse dettaglio(Long spazioLavoroId, Long utenteId, Long obiettivoId);

    ObiettivoRisparmioResponse crea(Long spazioLavoroId, Long utenteId, ObiettivoRisparmioRequest richiesta);

    ObiettivoRisparmioResponse aggiorna(Long spazioLavoroId, Long utenteId, Long obiettivoId,
            ObiettivoRisparmioRequest richiesta);

    void elimina(Long spazioLavoroId, Long utenteId, Long obiettivoId);

    /** Aggiunge denaro all'obiettivo */
    ObiettivoRisparmioResponse aggiungiVersamento(Long spazioLavoroId, Long utenteId, Long obiettivoId,
            VersamentoRequest richiesta);
}