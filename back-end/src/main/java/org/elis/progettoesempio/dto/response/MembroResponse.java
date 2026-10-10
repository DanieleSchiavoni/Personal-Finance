// MembroResponse.java
package org.elis.progettoesempio.dto.response;

import java.time.LocalDateTime;

import org.elis.progettoesempio.entity.enumerazioni.RuoloSpazio;

public record MembroResponse(
        Long utenteId,
        String username,
        String email,
        String nome,
        String cognome,
        RuoloSpazio ruolo,
        LocalDateTime dataIngresso) {
}