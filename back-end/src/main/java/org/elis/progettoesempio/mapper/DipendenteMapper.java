// DipendenteMapper.java
package org.elis.progettoesempio.mapper;

import org.elis.progettoesempio.dto.response.DipendenteResponse;
import org.elis.progettoesempio.dto.response.PagamentoStipendioResponse;
import org.elis.progettoesempio.entity.Dipendente;
import org.elis.progettoesempio.entity.Transazione;
import org.springframework.stereotype.Component;

@Component
public class DipendenteMapper {

    public DipendenteResponse aResponse(Dipendente dipendente) {
        return new DipendenteResponse(
                dipendente.getId(),
                dipendente.getNome(),
                dipendente.getCognome(),
                dipendente.getMansione(),
                dipendente.getStipendioMensile(),
                dipendente.getDataAssunzione(),
                dipendente.isAttivo());
    }

    /** Un pagamento di stipendio è una transazione con mese e anno di competenza */
    public PagamentoStipendioResponse aPagamentoResponse(Transazione pagamento) {
        return new PagamentoStipendioResponse(
                pagamento.getId(),
                pagamento.getMeseStipendio(),
                pagamento.getAnnoStipendio(),
                pagamento.getImporto(),
                pagamento.getDataTransazione());
    }
}