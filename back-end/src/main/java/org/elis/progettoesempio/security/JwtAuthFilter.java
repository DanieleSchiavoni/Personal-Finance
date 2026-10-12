// JwtAuthFilter.java
package org.elis.progettoesempio.security;

import java.io.IOException;
import java.util.List;

import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.repository.UtenteRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Legge il token dall'intestazione Authorization. Se è valido e l'utente esiste ed è abilitato,
 * la richiesta risulta autenticata. Altrimenti passa oltre senza autenticazione:
 * sarà la configurazione di sicurezza a rispondere 401.
 * Non è un @Component di proposito: lo crea SecurityConfig, così gira una volta sola.
 */
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFISSO = "Bearer ";

    private final JwtService jwtService;
    private final UtenteRepository utenteRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest richiesta, HttpServletResponse risposta,
            FilterChain catena) throws ServletException, IOException {

        String intestazione = richiesta.getHeader(HttpHeaders.AUTHORIZATION);
        if (intestazione != null && intestazione.startsWith(PREFISSO)) {
            String token = intestazione.substring(PREFISSO.length()).trim();

            // Ruolo e stato si rileggono ogni volta: un utente bloccato perde subito l'accesso
            jwtService.estraiUtenteId(token)
                    .flatMap(utenteRepository::findById)
                    .filter(Utente::isAbilitato)
                    .ifPresent(utente -> autentica(utente, richiesta));
        }
        catena.doFilter(richiesta, risposta);
    }

    private void autentica(Utente utente, HttpServletRequest richiesta) {
        var autorizzazioni = List.of(new SimpleGrantedAuthority("ROLE_" + utente.getRuolo().name()));
        // Come "principal" si usa l'id dell'utente: è quello che servirà ai controller
        var autenticazione = new UsernamePasswordAuthenticationToken(utente.getId(), null, autorizzazioni);
        autenticazione.setDetails(new WebAuthenticationDetailsSource().buildDetails(richiesta));
        SecurityContextHolder.getContext().setAuthentication(autenticazione);
    }
}