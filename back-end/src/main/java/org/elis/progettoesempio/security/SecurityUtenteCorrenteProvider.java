// SecurityUtenteCorrenteProvider.java
package org.elis.progettoesempio.security;

import org.elis.progettoesempio.exception.NonAutenticatoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Sostituisce il provider temporaneo: l'utente è quello autenticato dal token JWT */
@Component
public class SecurityUtenteCorrenteProvider implements UtenteCorrenteProvider {

    @Override
    public Long idUtenteCorrente() {
        Authentication autenticazione = SecurityContextHolder.getContext().getAuthentication();
        if (autenticazione != null && autenticazione.getPrincipal() instanceof Long utenteId) {
            return utenteId;
        }
        throw new NonAutenticatoException("Autenticazione richiesta");
    }
}