// CategoriaGlobaleRequest.java
package org.elis.progettoesempio.dto.request;

import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;

import jakarta.validation.constraints.*;

public record CategoriaGlobaleRequest(

        @NotBlank(message = "Il nome è obbligatorio")
        @Size(max = 100, message = "Il nome può avere al massimo 100 caratteri")
        String nome,

        @NotNull(message = "Il tipo è obbligatorio")
        TipoTransazione tipo,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Il colore deve avere il formato #RRGGBB")
        String colore,

        @Size(max = 255, message = "La descrizione può avere al massimo 255 caratteri")
        String descrizione,

        /** A chi è visibile: PERSONALE, AZIENDALE oppure vuoto per tutti */
        TipoSpazio tipoSpazio) {
}