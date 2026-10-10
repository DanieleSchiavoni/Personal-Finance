// SpazioLavoroMapper.java
package org.elis.progettoesempio.mapper;

import org.elis.progettoesempio.dto.response.MembroResponse;
import org.elis.progettoesempio.dto.response.SpazioLavoroResponse;
import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Utente;
import org.springframework.stereotype.Component;

@Component
public class SpazioLavoroMapper {

    /** Lo spazio visto dal membro: include il ruolo che il membro ha al suo interno */
    public SpazioLavoroResponse aResponse(MembroSpazio membro) {
        SpazioLavoro spazio = membro.getSpazioLavoro();
        return new SpazioLavoroResponse(
                spazio.getId(),
                spazio.getNome(),
                spazio.getTipo(),
                spazio.getValuta(),
                membro.getRuolo(),
                spazio.getDataCreazione());
    }

    public MembroResponse aMembroResponse(MembroSpazio membro) {
        Utente utente = membro.getUtente();
        return new MembroResponse(
                utente.getId(),
                utente.getUsername(),
                utente.getEmail(),
                utente.getNome(),
                utente.getCognome(),
                membro.getRuolo(),
                membro.getDataIngresso());
    }
}