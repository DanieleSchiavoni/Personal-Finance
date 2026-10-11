// BudgetRequest.java
package org.elis.progettoesempio.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

public record BudgetRequest(

        @NotNull(message = "La categoria è obbligatoria")
        Long categoriaId,

        @NotNull(message = "L'importo è obbligatorio")
        @Positive(message = "L'importo deve essere maggiore di zero")
        @Digits(integer = 10, fraction = 2, message = "L'importo può avere al massimo 2 decimali")
        BigDecimal importo,

        @NotNull(message = "Il mese è obbligatorio")
        @Min(value = 1, message = "Il mese deve essere compreso tra 1 e 12")
        @Max(value = 12, message = "Il mese deve essere compreso tra 1 e 12")
        Integer mese,

        @NotNull(message = "L'anno è obbligatorio")
        @Min(value = 2000, message = "Anno non valido")
        @Max(value = 2100, message = "Anno non valido")
        Integer anno) {
}