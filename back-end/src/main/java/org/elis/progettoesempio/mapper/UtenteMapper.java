// UtenteMapper.java
package org.elis.progettoesempio.mapper;

import org.elis.progettoesempio.dto.response.UtenteResponse;
import org.elis.progettoesempio.entity.Utente;
import org.springframework.stereotype.Component;

@Component
public class UtenteMapper {

    public UtenteResponse aResponse(Utente utente) {
        return new UtenteResponse(
                utente.getId(),
                utente.getUsername(),
                utente.getEmail(),
                utente.getNome(),
                utente.getCognome(),
                utente.getRuolo(),
                utente.isAbilitato(),
                utente.getDataCreazione());
    }
}