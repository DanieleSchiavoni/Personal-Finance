// PaginaResponse.java
package org.elis.progettoesempio.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

public record PaginaResponse<T>(
        List<T> contenuto,
        int pagina,
        int dimensione,
        long totaleElementi,
        int totalePagine) {

    public static <T> PaginaResponse<T> da(Page<T> pagina) {
        return new PaginaResponse<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}