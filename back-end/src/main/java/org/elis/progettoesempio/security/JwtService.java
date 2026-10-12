// JwtService.java
package org.elis.progettoesempio.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.elis.progettoesempio.entity.Utente;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Crea e verifica gli access token JWT (firmati con HMAC) */
@Service
public class JwtService {

    private static final int LUNGHEZZA_MINIMA_SEGRETO = 32;

    private final SecretKey chiave;
    private final long accessTtlMinuti;
    private final long refreshTtlGiorni;

    public JwtService(@Value("${app.jwt.secret:}") String segreto,
            @Value("${app.jwt.access-ttl-minuti:15}") long accessTtlMinuti,
            @Value("${app.jwt.refresh-ttl-giorni:7}") long refreshTtlGiorni) {
        // Si fallisce subito all'avvio: meglio che accettare token firmati con una chiave debole
        if (segreto == null || segreto.getBytes(StandardCharsets.UTF_8).length < LUNGHEZZA_MINIMA_SEGRETO) {
            throw new IllegalStateException(
                    "JWT_SECRET mancante o troppo corto: servono almeno 32 caratteri "
                    + "(ad esempio: export JWT_SECRET=$(openssl rand -base64 48))");
        }
        this.chiave = Keys.hmacShaKeyFor(segreto.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMinuti = accessTtlMinuti;
        this.refreshTtlGiorni = refreshTtlGiorni;
    }

    /** Il token contiene solo l'id dell'utente: ruolo e stato si leggono dal database a ogni richiesta */
    public String generaAccessToken(Utente utente) {
        Instant adesso = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(utente.getId()))
                .issuedAt(Date.from(adesso))
                .expiration(Date.from(adesso.plus(accessTtlMinuti, ChronoUnit.MINUTES)))
                .signWith(chiave)
                .compact();
    }

    /** L'id dell'utente, se il token è autentico e non scaduto; altrimenti vuoto */
    public Optional<Long> estraiUtenteId(String token) {
        try {
            Claims dati = Jwts.parser().verifyWith(chiave).build().parseSignedClaims(token).getPayload();
            return Optional.of(Long.valueOf(dati.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getAccessTtlSecondi() {
        return accessTtlMinuti * 60;
    }

    public long getRefreshTtlGiorni() {
        return refreshTtlGiorni;
    }
}