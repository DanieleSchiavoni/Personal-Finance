// DipendenteRequest.java
package org.elis.progettoesempio.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.*;

public record DipendenteRequest(

        @NotBlank(message = "Il nome è obbligatorio")
        @Size(max = 100, message = "Il nome può avere al massimo 100 caratteri")
        String nome,

        @NotBlank(message = "Il cognome è obbligatorio")
        @Size(max = 100, message = "Il cognome può avere al massimo 100 caratteri")
        String cognome,

        @Size(max = 100, message = "La mansione può avere al massimo 100 caratteri")
        String mansione,

        @NotNull(message = "Lo stipendio mensile è obbligatorio")
        @PositiveOrZero(message = "Lo stipendio non può essere negativo")
        @Digits(integer = 10, fraction = 2, message = "Lo stipendio può avere al massimo 2 decimali")
        BigDecimal stipendioMensile,

        LocalDate dataAssunzione,

        /** Facoltativo: in creazione se manca vale true, in modifica se manca resta invariato */
        Boolean attivo) {
}