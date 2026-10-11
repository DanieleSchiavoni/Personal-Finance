// AggiungiMembroRequest.java
package org.elis.progettoesempio.dto.request;



import org.elis.progettoesempio.entity.RuoloSpazio;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AggiungiMembroRequest(

        @NotBlank(message = "L'email è obbligatoria")
        @Email(message = "Email non valida")
        @Size(max = 150, message = "L'email può avere al massimo 150 caratteri")
        String email,

        @NotNull(message = "Il ruolo è obbligatorio")
        RuoloSpazio ruolo) {
}