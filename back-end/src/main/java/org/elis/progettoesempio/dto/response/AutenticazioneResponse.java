// dto/response/AutenticazioneResponse.java
package org.elis.progettoesempio.dto.response;

/** accessToken va nell'intestazione Authorization; refreshToken serve solo per ottenerne uno nuovo */
public record AutenticazioneResponse(
        String accessToken,
        String refreshToken,
        String tipoToken,
        long scadenzaAccessTokenSecondi,
        UtenteResponse utente) {
}