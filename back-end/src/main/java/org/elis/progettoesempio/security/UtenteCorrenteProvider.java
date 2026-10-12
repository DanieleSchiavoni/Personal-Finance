// UtenteCorrenteProvider.java
package org.elis.progettoesempio.security;

/** Fornisce l'id dell'utente che sta facendo la richiesta. Nella Fase 6 lo legge dal token JWT. */
public interface UtenteCorrenteProvider {

    /** @throws org.elis.progettoesempio.exception.NonAutenticatoException se l'utente non è identificato */
    Long idUtenteCorrente();
}