// PagamentoStipendioRequest.java
package org.elis.progettoesempio.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.elis.progettoesempio.entity.enumerazioni.MetodoPagamento;

import jakarta.validation.constraints.*;

/** Mese e anno sono obbligatori; se gli altri campi mancano si usano i valori predefiniti */
public record PagamentoStipendioRequest(

        @NotNull(message = "Il mese è obbligatorio")
        @Min(value = 1, message = "Il mese deve essere compreso tra 1 e 12")
        @Max(value = 12, message = "Il mese deve essere compreso tra 1 e 12")
        Integer mese,

        @NotNull(message = "L'anno è obbligatorio")
        @Min(value = 2000, message = "Anno non valido")
        @Max(value = 2100, message = "Anno non valido")
        Integer anno,

        /** Predefinita: oggi */
        LocalDate dataPagamento,

        /** Predefinito: lo stipendio mensile del dipendente */
        @Positive(message = "L'importo deve essere maggiore di zero")
        @Digits(integer = 10, fraction = 2, message = "L'importo può avere al massimo 2 decimali")
        BigDecimal importo,

        /** Predefinito: bonifico */
        MetodoPagamento metodoPagamento) {
}