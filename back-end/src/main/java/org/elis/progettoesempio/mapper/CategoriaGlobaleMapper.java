// CategoriaGlobaleMapper.java
package org.elis.progettoesempio.mapper;

import org.elis.progettoesempio.dto.response.CategoriaGlobaleResponse;
import org.elis.progettoesempio.entity.Categoria;
import org.springframework.stereotype.Component;

@Component
public class CategoriaGlobaleMapper {

    public CategoriaGlobaleResponse aResponse(Categoria categoria) {
        return new CategoriaGlobaleResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getTipo(),
                categoria.getColore(),
                categoria.getDescrizione(),
                categoria.getTipoSpazio());
    }
}