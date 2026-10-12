// dto/request/RinnovaTokenRequest.java  (usato per refresh e logout)
package org.elis.progettoesempio.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RinnovaTokenRequest(

        @NotBlank(message = "Il refresh token è obbligatorio")
        String refreshToken) {
}