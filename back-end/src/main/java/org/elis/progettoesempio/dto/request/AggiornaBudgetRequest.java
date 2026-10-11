// AggiornaBudgetRequest.java
package org.elis.progettoesempio.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

/** Di un budget esistente si può cambiare solo l'importo (categoria e mese sono la sua identità) */
public record AggiornaBudgetRequest(

        @NotNull(message = "L'importo è obbligatorio")
        @Positive(message = "L'importo deve essere maggiore di zero")
        @Digits(integer = 10, fraction = 2, message = "L'importo può avere al massimo 2 decimali")
        BigDecimal importo) {
}