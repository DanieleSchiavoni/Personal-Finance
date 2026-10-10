// CategoriaMapper.java
package org.elis.progettoesempio.mapper;

import org.elis.progettoesempio.dto.response.CategoriaResponse;
import org.elis.progettoesempio.entity.Categoria;
import org.springframework.stereotype.Component;

@Component
public class CategoriaMapper {

    public CategoriaResponse aResponse(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getTipo(),
                categoria.getColore(),
                categoria.getDescrizione(),
                categoria.getSpazioLavoro() == null);
    }
}