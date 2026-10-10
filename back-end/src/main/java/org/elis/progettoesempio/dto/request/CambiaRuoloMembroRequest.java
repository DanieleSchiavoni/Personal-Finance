// CambiaRuoloMembroRequest.java
package org.elis.progettoesempio.dto.request;

import org.elis.progettoesempio.entity.enumerazioni.RuoloSpazio;

import jakarta.validation.constraints.NotNull;

public record CambiaRuoloMembroRequest(

        @NotNull(message = "Il ruolo è obbligatorio")
        RuoloSpazio ruolo) {
}