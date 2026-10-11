// ObiettivoRisparmioMapper.java
package org.elis.progettoesempio.mapper;

import java.math.BigDecimal;

import org.elis.progettoesempio.dto.response.ObiettivoRisparmioResponse;
import org.elis.progettoesempio.entity.ObiettivoRisparmio;
import org.elis.progettoesempio.entity.enumerazioni.StatoObiettivo;
import org.springframework.stereotype.Component;

@Component
public class ObiettivoRisparmioMapper {

    /** I valori calcolati (percentuale, stato, importo mancante) li produce il service */
    public ObiettivoRisparmioResponse aResponse(ObiettivoRisparmio obiettivo, BigDecimal importoMancante,
            BigDecimal percentuale, StatoObiettivo stato) {
        return new ObiettivoRisparmioResponse(
                obiettivo.getId(),
                obiettivo.getNome(),
                obiettivo.getDescrizione(),
                obiettivo.getImportoObiettivo(),
                obiettivo.getImportoAttuale(),
                importoMancante,
                percentuale,
                obiettivo.getScadenza(),
                stato);
    }
}