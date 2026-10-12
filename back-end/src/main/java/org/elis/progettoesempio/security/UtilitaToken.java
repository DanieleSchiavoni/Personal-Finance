// UtilitaToken.java
package org.elis.progettoesempio.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Generazione e hash dei refresh token */
public final class UtilitaToken {

    private static final SecureRandom GENERATORE = new SecureRandom();

    private UtilitaToken() {
    }

    /** 256 bit casuali, in una forma sicura da usare nei JSON e negli URL */
    public static String generaCasuale() {
        byte[] byteCasuali = new byte[32];
        GENERATORE.nextBytes(byteCasuali);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(byteCasuali);
    }

    /** Nel database si salva solo l'hash: il token vero lo conosce solo il client */
    public static String calcolaHash(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponibile", e);
        }
    }
}