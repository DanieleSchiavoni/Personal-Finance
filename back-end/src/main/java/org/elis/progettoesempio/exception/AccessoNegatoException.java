// AccessoNegatoException.java  (diventerà 403)
package org.elis.progettoesempio.exception;

public class AccessoNegatoException extends RuntimeException {
    public AccessoNegatoException(String messaggio) {
        super(messaggio);
    }
}