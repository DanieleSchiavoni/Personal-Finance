// service/AdminService.java
package org.elis.progettoesempio.service;

import java.util.List;

import org.elis.progettoesempio.dto.request.CategoriaGlobaleRequest;
import org.elis.progettoesempio.dto.request.RicercaUtentiRequest;
import org.elis.progettoesempio.dto.response.CategoriaGlobaleResponse;
import org.elis.progettoesempio.dto.response.PaginaResponse;
import org.elis.progettoesempio.dto.response.StatisticheGeneraliResponse;
import org.elis.progettoesempio.dto.response.UtenteResponse;

/** Ogni metodo verifica che adminId sia un amministratore abilitato (altrimenti 403) */
public interface AdminService {

    PaginaResponse<UtenteResponse> elencaUtenti(Long adminId, RicercaUtentiRequest ricerca);

    /** Blocca (false) o sblocca (true) un utente. Bloccando si revocano i suoi refresh token. */
    UtenteResponse impostaAbilitazione(Long adminId, Long utenteId, boolean abilitato);

    List<CategoriaGlobaleResponse> elencaCategorieGlobali(Long adminId);

    CategoriaGlobaleResponse creaCategoriaGlobale(Long adminId, CategoriaGlobaleRequest richiesta);

    CategoriaGlobaleResponse aggiornaCategoriaGlobale(Long adminId, Long categoriaId, CategoriaGlobaleRequest richiesta);

    void eliminaCategoriaGlobale(Long adminId, Long categoriaId);

    StatisticheGeneraliResponse statistiche(Long adminId);
}