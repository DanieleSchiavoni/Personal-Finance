// NotificaResponse.java
package org.elis.progettoesempio.dto.response;

import java.time.LocalDateTime;

public record NotificaResponse(
        Long id,
        String titolo,
        String messaggio,
        LocalDateTime data,
        boolean letta) {
}