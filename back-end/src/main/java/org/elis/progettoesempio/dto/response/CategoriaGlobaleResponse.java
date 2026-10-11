// CategoriaGlobaleResponse.java
package org.elis.progettoesempio.dto.response;

import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;

public record CategoriaGlobaleResponse(
        Long id,
        String nome,
        TipoTransazione tipo,
        String colore,
        String descrizione,
        TipoSpazio tipoSpazio) {
}