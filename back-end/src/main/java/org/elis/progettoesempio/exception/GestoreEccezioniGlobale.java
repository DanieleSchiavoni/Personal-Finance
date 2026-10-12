package org.elis.progettoesempio.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.elis.progettoesempio.dto.response.ErroreResponse;
import org.elis.progettoesempio.dto.response.ErroreResponse.ErroreCampo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GestoreEccezioniGlobale {

    @ExceptionHandler(RisorsaNonTrovataException.class)
    public ResponseEntity<ErroreResponse> nonTrovata(RisorsaNonTrovataException e, HttpServletRequest richiesta) {
        return risposta(HttpStatus.NOT_FOUND, "NON_TROVATO", e.getMessage(), richiesta, null);
    }

    @ExceptionHandler(ConflittoException.class)
    public ResponseEntity<ErroreResponse> conflitto(ConflittoException e, HttpServletRequest richiesta) {
        return risposta(HttpStatus.CONFLICT, "CONFLITTO", e.getMessage(), richiesta, null);
    }

    @ExceptionHandler(AccessoNegatoException.class)
    public ResponseEntity<ErroreResponse> accessoNegato(AccessoNegatoException e, HttpServletRequest richiesta) {
        return risposta(HttpStatus.FORBIDDEN, "ACCESSO_NEGATO", e.getMessage(), richiesta, null);
    }

    @ExceptionHandler(OperazioneNonValidaException.class)
    public ResponseEntity<ErroreResponse> operazioneNonValida(OperazioneNonValidaException e,
            HttpServletRequest richiesta) {
        return risposta(HttpStatus.BAD_REQUEST, "RICHIESTA_NON_VALIDA", e.getMessage(), richiesta, null);
    }

    @ExceptionHandler(NonAutenticatoException.class)
    public ResponseEntity<ErroreResponse> nonAutenticato(NonAutenticatoException e, HttpServletRequest richiesta) {
        return risposta(HttpStatus.UNAUTHORIZED, "NON_AUTENTICATO", e.getMessage(), richiesta, null);
    }

    /** Errori di Bean Validation sul corpo (@RequestBody) e sui parametri della richiesta (@ModelAttribute) */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErroreResponse> validazione(BindException e, HttpServletRequest richiesta) {
        List<ErroreCampo> campi = e.getBindingResult().getFieldErrors().stream()
                .map(errore -> new ErroreCampo(errore.getField(), errore.getDefaultMessage()))
                .toList();
        return risposta(HttpStatus.BAD_REQUEST, "ERRORE_VALIDAZIONE", "Dati non validi", richiesta, campi);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroreResponse> parametroNonValido(MethodArgumentTypeMismatchException e,
            HttpServletRequest richiesta) {
        return risposta(HttpStatus.BAD_REQUEST, "RICHIESTA_NON_VALIDA",
                "Il parametro '" + e.getName() + "' non è valido", richiesta, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroreResponse> corpoNonLeggibile(HttpMessageNotReadableException e,
            HttpServletRequest richiesta) {
        return risposta(HttpStatus.BAD_REQUEST, "RICHIESTA_NON_VALIDA",
                "Corpo della richiesta mancante o malformato", richiesta, null);
    }

    /** Rete di sicurezza del database, ad esempio per due richieste identiche arrivate nello stesso istante */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroreResponse> integritaDati(DataIntegrityViolationException e,
            HttpServletRequest richiesta) {
        log.warn("Violazione di integrità dei dati su {}", richiesta.getRequestURI(), e);
        return risposta(HttpStatus.CONFLICT, "CONFLITTO",
                "L'operazione è in conflitto con i dati esistenti", richiesta, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroreResponse> erroreGenerico(Exception e, HttpServletRequest richiesta) {
        // Le eccezioni standard di Spring MVC (404, 405, 415...) portano già il loro codice di stato
        if (e instanceof ErrorResponse erroreSpring) {
            HttpStatus stato = HttpStatus.valueOf(erroreSpring.getStatusCode().value());
            return risposta(stato, codiceStandard(stato), messaggioStandard(stato), richiesta, null);
        }
        log.error("Errore non gestito su {}", richiesta.getRequestURI(), e);
        return risposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRORE_INTERNO",
                "Si è verificato un errore interno", richiesta, null);
    }

    private ResponseEntity<ErroreResponse> risposta(HttpStatus stato, String codice, String messaggio,
            HttpServletRequest richiesta, List<ErroreCampo> campi) {
        ErroreResponse corpo = new ErroreResponse(
                LocalDateTime.now(), stato.value(), codice, messaggio, richiesta.getRequestURI(), campi);
        return ResponseEntity.status(stato).body(corpo);
    }

    private String codiceStandard(HttpStatus stato) {
        return switch (stato.value()) {
            case 404 -> "NON_TROVATO";
            case 405 -> "METODO_NON_SUPPORTATO";
            case 415 -> "FORMATO_NON_SUPPORTATO";
            default -> "RICHIESTA_NON_VALIDA";
        };
    }

    private String messaggioStandard(HttpStatus stato) {
        return switch (stato.value()) {
            case 404 -> "Risorsa non trovata";
            case 405 -> "Metodo HTTP non supportato per questo percorso";
            case 415 -> "Formato del contenuto non supportato";
            default -> "Richiesta non valida";
        };
    }
}