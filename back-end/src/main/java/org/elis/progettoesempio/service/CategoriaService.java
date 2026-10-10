// service/CategoriaService.java
package org.elis.progettoesempio.service;

import java.util.List;

import org.elis.progettoesempio.dto.request.CategoriaRequest;
import org.elis.progettoesempio.dto.response.CategoriaResponse;

public interface CategoriaService {

    /** Categorie dello spazio + globali adatte al suo tipo */
    List<CategoriaResponse> elenca(Long spazioLavoroId, Long utenteId);

    CategoriaResponse crea(Long spazioLavoroId, Long utenteId, CategoriaRequest richiesta);

    CategoriaResponse aggiorna(Long spazioLavoroId, Long utenteId, Long categoriaId, CategoriaRequest richiesta);

    void elimina(Long spazioLavoroId, Long utenteId, Long categoriaId);
}