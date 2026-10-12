// dto/request/LoginRequest.java
package org.elis.progettoesempio.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        /** Username oppure email */
        @NotBlank(message = "Username o email obbligatori")
        String identificativo,

        @NotBlank(message = "La password è obbligatoria")
        String password) {
}