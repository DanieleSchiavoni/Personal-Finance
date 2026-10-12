// service/AutenticazioneService.java
package org.elis.progettoesempio.service;

import org.elis.progettoesempio.dto.request.CambiaPasswordRequest;
import org.elis.progettoesempio.dto.request.ConfermaPasswordRequest;
import org.elis.progettoesempio.dto.request.LoginRequest;
import org.elis.progettoesempio.dto.request.RegistrazioneRequest;
import org.elis.progettoesempio.dto.request.RinnovaTokenRequest;
import org.elis.progettoesempio.dto.response.AutenticazioneResponse;

public interface AutenticazioneService {

    /** Crea l'utente (sempre con ruolo UTENTE) e il suo spazio personale, e lo autentica subito */
    AutenticazioneResponse registra(RegistrazioneRequest richiesta);

    AutenticazioneResponse accedi(LoginRequest richiesta);

    /** Scambia un refresh token con una nuova coppia di token; il vecchio non vale più */
    AutenticazioneResponse rinnova(RinnovaTokenRequest richiesta);

    /** Revoca il refresh token */
    void esci(RinnovaTokenRequest richiesta);

    /** Richiede la password attuale; revoca tutte le sessioni aperte */
    void cambiaPassword(Long utenteId, CambiaPasswordRequest richiesta);

    /** Richiede la password come conferma, poi elimina l'account */
    void eliminaAccount(Long utenteId, ConfermaPasswordRequest richiesta);
}