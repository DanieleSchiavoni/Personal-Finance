// OperazioneNonValidaException.java  (diventerà 400)
package org.elis.progettoesempio.exception;

public class OperazioneNonValidaException extends RuntimeException {
    public OperazioneNonValidaException(String messaggio) {
        super(messaggio);
    }
}