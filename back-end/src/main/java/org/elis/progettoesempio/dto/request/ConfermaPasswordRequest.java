// dto/request/ConfermaPasswordRequest.java
package org.elis.progettoesempio.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConfermaPasswordRequest(

        @NotBlank(message = "La password è obbligatoria")
        String password) {
}