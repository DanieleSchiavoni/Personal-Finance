// TransazioneRequest.java
package org.elis.progettoesempio.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.elis.progettoesempio.entity.enumerazioni.MetodoPagamento;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;

import jakarta.validation.constraints.*;

public record TransazioneRequest(

        @NotNull(message = "L'importo è obbligatorio")
        @Positive(message = "L'importo deve essere maggiore di zero")
        @Digits(integer = 10, fraction = 2, message = "L'importo può avere al massimo 2 decimali")
        BigDecimal importo,

        @NotBlank(message = "La descrizione è obbligatoria")
        @Size(max = 255, message = "La descrizione può avere al massimo 255 caratteri")
        String descrizione,

        @NotNull(message = "La data è obbligatoria")
        LocalDate data,

        @NotNull(message = "Il tipo è obbligatorio")
        TipoTransazione tipo,

        @NotNull(message = "Il metodo di pagamento è obbligatorio")
        MetodoPagamento metodoPagamento,

        @NotNull(message = "La categoria è obbligatoria")
        Long categoriaId,

        @Size(max = 500, message = "Le note possono avere al massimo 500 caratteri")
        String note) {
}