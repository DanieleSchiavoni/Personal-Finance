// GestoreErroriSicurezza.java
package org.elis.progettoesempio.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.elis.progettoesempio.dto.response.ErroreResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Risponde 401 (non autenticato) e 403 (ruolo insufficiente) nello stesso formato degli altri errori.
 * Questi errori nascono nei filtri, prima dei controller: non passano da GestoreEccezioniGlobale.
 */
@Component
@RequiredArgsConstructor
public class GestoreErroriSicurezza implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest richiesta, HttpServletResponse risposta,
            AuthenticationException eccezione) throws IOException {
        scrivi(richiesta, risposta, HttpStatus.UNAUTHORIZED, "NON_AUTENTICATO", "Autenticazione richiesta");
    }

    @Override
    public void handle(HttpServletRequest richiesta, HttpServletResponse risposta,
            AccessDeniedException eccezione) throws IOException {
        scrivi(richiesta, risposta, HttpStatus.FORBIDDEN, "ACCESSO_NEGATO",
                "Non hai i permessi per eseguire questa operazione");
    }

    private void scrivi(HttpServletRequest richiesta, HttpServletResponse risposta, HttpStatus stato,
            String codice, String messaggio) throws IOException {
        risposta.setStatus(stato.value());
        risposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErroreResponse corpo = new ErroreResponse(
                LocalDateTime.now(), stato.value(), codice, messaggio, richiesta.getRequestURI(), null);
        objectMapper.writeValue(risposta.getOutputStream(), corpo);
    }
}