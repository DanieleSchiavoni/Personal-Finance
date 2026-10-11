// service/NotificaService.java
package org.elis.progettoesempio.service;

import java.util.List;

import org.elis.progettoesempio.dto.response.NotificaResponse;
import org.elis.progettoesempio.entity.Utente;

public interface NotificaService {

    List<NotificaResponse> elenca(Long utenteId);

    long contaNonLette(Long utenteId);

    NotificaResponse segnaComeLetta(Long utenteId, Long notificaId);

    void segnaTutteComeLette(Long utenteId);

    void elimina(Long utenteId, Long notificaId);

    /** Uso interno: gli altri service creano notifiche per un utente */
    void creaPerUtente(Utente destinatario, String titolo, String messaggio);
}