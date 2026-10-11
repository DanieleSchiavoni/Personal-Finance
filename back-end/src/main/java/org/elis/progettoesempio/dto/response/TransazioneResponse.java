// TransazioneResponse.java
package org.elis.progettoesempio.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.elis.progettoesempio.entity.enumerazioni.MetodoPagamento;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;

public record TransazioneResponse(
        Long id,
        BigDecimal importo,
        String descrizione,
        LocalDate data,
        TipoTransazione tipo,
        MetodoPagamento metodoPagamento,
        String note,
        Long categoriaId,
        String categoriaNome,
        String categoriaColore,
        Long creataDaId,
        String creataDaUsername) {
}