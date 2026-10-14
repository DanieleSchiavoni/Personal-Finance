// dto/response/NotiziaResponse.java
package org.elis.progettoesempio.dto.response;

import java.time.Instant;

/** immagineUrl e pubblicataIl possono mancare, se il feed non li fornisce */
public record NotiziaResponse(
        String titolo,
        String sintesi,
        String url,
        String fonte,
        String lingua,
        String immagineUrl,
        Instant pubblicataIl) {
}