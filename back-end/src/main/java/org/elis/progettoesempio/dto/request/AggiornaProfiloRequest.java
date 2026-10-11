// AggiornaProfiloRequest.java
package org.elis.progettoesempio.dto.request;

import jakarta.validation.constraints.*;

public record AggiornaProfiloRequest(

        @NotBlank(message = "Lo username è obbligatorio")
        @Size(min = 3, max = 50, message = "Lo username deve avere tra 3 e 50 caratteri")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                 message = "Lo username può contenere solo lettere, numeri, punto, trattino e underscore")
        String username,

        @NotBlank(message = "L'email è obbligatoria")
        @Email(message = "Email non valida")
        @Size(max = 150, message = "L'email può avere al massimo 150 caratteri")
        String email,

        @NotBlank(message = "Il nome è obbligatorio")
        @Size(max = 100, message = "Il nome può avere al massimo 100 caratteri")
        String nome,

        @NotBlank(message = "Il cognome è obbligatorio")
        @Size(max = 100, message = "Il cognome può avere al massimo 100 caratteri")
        String cognome) {
}