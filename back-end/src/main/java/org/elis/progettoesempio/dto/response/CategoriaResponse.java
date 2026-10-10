// CategoriaResponse.java
package org.elis.progettoesempio.dto.response;

import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;

/** globale = true per le categorie predefinite, che i membri non possono modificare */
public record CategoriaResponse(
        Long id,
        String nome,
        TipoTransazione tipo,
        String colore,
        String descrizione,
        boolean globale) {
}