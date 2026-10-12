// dto/request/CambiaPasswordRequest.java
package org.elis.progettoesempio.dto.request;

import jakarta.validation.constraints.*;

public record CambiaPasswordRequest(

        @NotBlank(message = "La password attuale è obbligatoria")
        String passwordAttuale,

        @NotBlank(message = "La nuova password è obbligatoria")
        @Size(min = 8, max = 72, message = "La password deve avere tra 8 e 72 caratteri")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                 message = "La password deve contenere almeno una lettera e un numero")
        String nuovaPassword) {
}