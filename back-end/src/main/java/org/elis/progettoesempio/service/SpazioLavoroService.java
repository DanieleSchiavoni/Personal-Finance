// service/SpazioLavoroService.java
package org.elis.progettoesempio.service;

import java.util.List;

import org.elis.progettoesempio.dto.request.AggiornaSpazioLavoroRequest;
import org.elis.progettoesempio.dto.request.AggiungiMembroRequest;
import org.elis.progettoesempio.dto.request.CambiaRuoloMembroRequest;
import org.elis.progettoesempio.dto.request.CreaSpazioLavoroRequest;
import org.elis.progettoesempio.dto.response.MembroResponse;
import org.elis.progettoesempio.dto.response.SpazioLavoroResponse;
import org.elis.progettoesempio.entity.Utente;

public interface SpazioLavoroService {

    /** Chiamato alla registrazione (Fase 6): ogni utente ha uno spazio personale */
    SpazioLavoroResponse creaSpazioPersonale(Utente utente);

    SpazioLavoroResponse creaSpazioAziendale(Long utenteId, CreaSpazioLavoroRequest richiesta);

    /** Spazi a cui l'utente appartiene (personale per primo) */
    List<SpazioLavoroResponse> elencaSpaziUtente(Long utenteId);

    SpazioLavoroResponse dettaglio(Long spazioLavoroId, Long utenteId);

    SpazioLavoroResponse aggiorna(Long spazioLavoroId, Long utenteId, AggiornaSpazioLavoroRequest richiesta);

    void elimina(Long spazioLavoroId, Long utenteId);

    List<MembroResponse> elencaMembri(Long spazioLavoroId, Long utenteId);

    MembroResponse aggiungiMembro(Long spazioLavoroId, Long utenteId, AggiungiMembroRequest richiesta);

    MembroResponse cambiaRuoloMembro(Long spazioLavoroId, Long utenteId, Long membroUtenteId,
            CambiaRuoloMembroRequest richiesta);

    void rimuoviMembro(Long spazioLavoroId, Long utenteId, Long membroUtenteId);
}