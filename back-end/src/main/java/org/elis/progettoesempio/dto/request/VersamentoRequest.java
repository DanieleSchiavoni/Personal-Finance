// VersamentoRequest.java
package org.elis.progettoesempio.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

public record VersamentoRequest(

        @NotNull(message = "L'importo è obbligatorio")
        @Positive(message = "L'importo deve essere maggiore di zero")
        @Digits(integer = 10, fraction = 2, message = "L'importo può avere al massimo 2 decimali")
        BigDecimal importo) {
}