// AggiornaSpazioLavoroRequest.java
package org.elis.progettoesempio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AggiornaSpazioLavoroRequest(

        @NotBlank(message = "Il nome è obbligatorio")
        @Size(max = 100, message = "Il nome può avere al massimo 100 caratteri")
        String nome,

        @NotBlank(message = "La valuta è obbligatoria")
        @Pattern(regexp = "^[A-Z]{3}$", message = "La valuta deve essere un codice di 3 lettere maiuscole (es. EUR)")
        String valuta) {
}