// ObiettivoRisparmioRequest.java
package org.elis.progettoesempio.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.*;

public record ObiettivoRisparmioRequest(

        @NotBlank(message = "Il nome è obbligatorio")
        @Size(max = 100, message = "Il nome può avere al massimo 100 caratteri")
        String nome,

        @Size(max = 255, message = "La descrizione può avere al massimo 255 caratteri")
        String descrizione,

        @NotNull(message = "L'importo dell'obiettivo è obbligatorio")
        @Positive(message = "L'importo deve essere maggiore di zero")
        @Digits(integer = 10, fraction = 2, message = "L'importo può avere al massimo 2 decimali")
        BigDecimal importoObiettivo,

        // Facoltativa
        LocalDate scadenza) {
}