// SpazioLavoroResponse.java
package org.elis.progettoesempio.dto.response;

import java.time.LocalDateTime;

import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;

/** ruoloUtente è il ruolo di chi fa la richiesta: il frontend lo usa per nascondere i pulsanti non permessi */
public record SpazioLavoroResponse(
        Long id,
        String nome,
        TipoSpazio tipo,
        String valuta,
        RuoloSpazio ruoloUtente,
        LocalDateTime dataCreazione) {
}