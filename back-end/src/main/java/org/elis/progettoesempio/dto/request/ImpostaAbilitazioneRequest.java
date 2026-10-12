// dto/request/ImpostaAbilitazioneRequest.java
package org.elis.progettoesempio.dto.request;

import jakarta.validation.constraints.NotNull;

public record ImpostaAbilitazioneRequest(

        @NotNull(message = "Il campo abilitato è obbligatorio")
        Boolean abilitato) {
}