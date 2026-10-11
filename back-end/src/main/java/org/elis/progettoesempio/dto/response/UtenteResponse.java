// UtenteResponse.java
package org.elis.progettoesempio.dto.response;

import java.time.LocalDateTime;

import org.elis.progettoesempio.entity.enumerazioni.RuoloUtente;

/** Mai la password, nemmeno cifrata */
public record UtenteResponse(
        Long id,
        String username,
        String email,
        String nome,
        String cognome,
        RuoloUtente ruolo,
        boolean abilitato,
        LocalDateTime dataCreazione) {
}