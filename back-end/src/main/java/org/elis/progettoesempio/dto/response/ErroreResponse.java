// dto/response/ErroreResponse.java
package org.elis.progettoesempio.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Risposta standard di tutti gli errori. "campi" compare solo per gli errori di validazione. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroreResponse(
        LocalDateTime timestamp,
        int status,
        String errore,
        String messaggio,
        String percorso,
        List<ErroreCampo> campi) {

    public record ErroreCampo(String campo, String messaggio) {
    }
}