// CreaSpazioLavoroRequest.java
package org.elis.progettoesempio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreaSpazioLavoroRequest(

        @NotBlank(message = "Il nome è obbligatorio")
        @Size(max = 100, message = "Il nome può avere al massimo 100 caratteri")
        String nome,

        // Facoltativa: se manca si usa EUR
        @Pattern(regexp = "^[A-Z]{3}$", message = "La valuta deve essere un codice di 3 lettere maiuscole (es. EUR)")
        String valuta) {
}