// RicercaUtentiRequest.java
package org.elis.progettoesempio.dto.request;

import jakarta.validation.constraints.*;

/** Ricerca degli utenti per gli admin. Tutti i campi sono facoltativi. */
public record RicercaUtentiRequest(

        /** Cerca in username, email, nome e cognome */
        @Size(max = 100, message = "Il testo di ricerca può avere al massimo 100 caratteri")
        String testo,

        @PositiveOrZero(message = "La pagina non può essere negativa")
        Integer pagina,

        @Min(value = 1, message = "La dimensione minima è 1")
        @Max(value = 100, message = "La dimensione massima è 100")
        Integer dimensione) {
}