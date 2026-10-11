// RicercaTransazioniRequest.java
package org.elis.progettoesempio.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.elis.progettoesempio.entity.enumerazioni.MetodoPagamento;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;

import jakarta.validation.constraints.*;

/** Filtri, paginazione e ordinamento della ricerca. Tutti i campi sono facoltativi. */
public record RicercaTransazioniRequest(
        LocalDate dal,
        LocalDate al,
        Long categoriaId,
        TipoTransazione tipo,
        MetodoPagamento metodoPagamento,

        @PositiveOrZero(message = "L'importo minimo non può essere negativo")
        BigDecimal importoMin,

        @PositiveOrZero(message = "L'importo massimo non può essere negativo")
        BigDecimal importoMax,

        /** Cerca nella descrizione e nelle note */
        @Size(max = 100, message = "Il testo di ricerca può avere al massimo 100 caratteri")
        String testo,

        @PositiveOrZero(message = "La pagina non può essere negativa")
        Integer pagina,

        @Min(value = 1, message = "La dimensione minima è 1")
        @Max(value = 100, message = "La dimensione massima è 100")
        Integer dimensione,

        @Pattern(regexp = "(?i)data|importo|descrizione|categoria",
                 message = "Ordinamento ammesso per: data, importo, descrizione, categoria")
        String ordinaPer,

        @Pattern(regexp = "(?i)asc|desc", message = "La direzione deve essere asc o desc")
        String direzione) {
}