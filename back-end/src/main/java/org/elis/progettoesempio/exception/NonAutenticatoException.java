package org.elis.progettoesempio.exception;

public class NonAutenticatoException extends RuntimeException {
    public NonAutenticatoException(String messaggio) {
        super(messaggio);
    }
}