// service/impl/AutenticazioneServiceImpl.java
package org.elis.progettoesempio.service.impl;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import org.elis.progettoesempio.dto.request.CambiaPasswordRequest;
import org.elis.progettoesempio.dto.request.ConfermaPasswordRequest;
import org.elis.progettoesempio.dto.request.LoginRequest;
import org.elis.progettoesempio.dto.request.RegistrazioneRequest;
import org.elis.progettoesempio.dto.request.RinnovaTokenRequest;
import org.elis.progettoesempio.dto.response.AutenticazioneResponse;
import org.elis.progettoesempio.entity.RefreshToken;
import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.exception.AccessoNegatoException;
import org.elis.progettoesempio.exception.ConflittoException;
import org.elis.progettoesempio.exception.NonAutenticatoException;
import org.elis.progettoesempio.exception.OperazioneNonValidaException;
import org.elis.progettoesempio.exception.RisorsaNonTrovataException;
import org.elis.progettoesempio.mapper.UtenteMapper;
import org.elis.progettoesempio.repository.RefreshTokenRepository;
import org.elis.progettoesempio.repository.UtenteRepository;
import org.elis.progettoesempio.security.JwtService;
import org.elis.progettoesempio.security.UtilitaToken;
import org.elis.progettoesempio.service.AutenticazioneService;
import org.elis.progettoesempio.service.SpazioLavoroService;
import org.elis.progettoesempio.service.UtenteService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AutenticazioneServiceImpl implements AutenticazioneService {

    /**
     * Hash di una password inesistente. Serve nel login quando l'utente non c'è:
     * si fa comunque un confronto BCrypt, così chi sbaglia username non si distingue dai tempi di risposta.
     */
    private static final String HASH_FITTIZIO = new BCryptPasswordEncoder().encode("password-fittizia-1");

    private final UtenteRepository utenteRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SpazioLavoroService spazioLavoroService;
    private final UtenteService utenteService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UtenteMapper utenteMapper;

    @Override
    @Transactional
    public AutenticazioneResponse registra(RegistrazioneRequest richiesta) {
        String username = richiesta.username().trim();
        String email = richiesta.email().trim().toLowerCase(Locale.ROOT);

        if (utenteRepository.existsByUsername(username)) {
            throw new ConflittoException("Username già in uso");
        }
        if (utenteRepository.existsByEmail(email)) {
            throw new ConflittoException("Email già in uso");
        }

        Utente utente = new Utente();
        utente.setUsername(username);
        utente.setEmail(email);
        utente.setPassword(passwordEncoder.encode(richiesta.password()));
        utente.setNome(richiesta.nome().trim());
        utente.setCognome(richiesta.cognome().trim());
        // Ruolo UTENTE e abilitato=true sono i valori predefiniti: il ruolo non arriva mai dalla richiesta
        utenteRepository.save(utente);

        spazioLavoroService.creaSpazioPersonale(utente);
        return emettiTokens(utente);
    }

    @Override
    @Transactional
    public AutenticazioneResponse accedi(LoginRequest richiesta) {
        String identificativo = richiesta.identificativo().trim();
        Optional<Utente> trovato = identificativo.contains("@")
                ? utenteRepository.findByEmail(identificativo.toLowerCase(Locale.ROOT))
                : utenteRepository.findByUsername(identificativo);

        // Il confronto della password avviene sempre, anche se l'utente non esiste
        String hash = trovato.map(Utente::getPassword).orElse(HASH_FITTIZIO);
        boolean passwordCorretta = passwordEncoder.matches(richiesta.password(), hash);

        // Stesso messaggio per utente inesistente e password sbagliata: non si rivela quali username esistono
        if (trovato.isEmpty() || !passwordCorretta) {
            throw new NonAutenticatoException("Credenziali non valide");
        }

        Utente utente = trovato.get();
        if (!utente.isAbilitato()) {
            throw new AccessoNegatoException("Account disabilitato");
        }
        return emettiTokens(utente);
    }

    @Override
    @Transactional
    public AutenticazioneResponse rinnova(RinnovaTokenRequest richiesta) {
        RefreshToken salvato = refreshTokenRepository.findByToken(UtilitaToken.calcolaHash(richiesta.refreshToken()))
                .orElseThrow(() -> new NonAutenticatoException("Refresh token non valido"));

        if (salvato.getDataScadenza().isBefore(LocalDateTime.now())) {
            throw new NonAutenticatoException("Refresh token scaduto");
        }
        Utente utente = salvato.getUtente();
        if (!utente.isAbilitato()) {
            throw new NonAutenticatoException("Account disabilitato");
        }

        // Rotazione: il refresh token si usa una volta sola, subito sostituito da uno nuovo
        refreshTokenRepository.delete(salvato);
        return emettiTokens(utente);
    }

    @Override
    @Transactional
    public void esci(RinnovaTokenRequest richiesta) {
        // Se il token non esiste (già usato o scaduto) il risultato è lo stesso: la sessione è chiusa
        refreshTokenRepository.findByToken(UtilitaToken.calcolaHash(richiesta.refreshToken()))
                .ifPresent(refreshTokenRepository::delete);
    }

    @Override
    @Transactional
    public void cambiaPassword(Long utenteId, CambiaPasswordRequest richiesta) {
        Utente utente = trovaUtente(utenteId);
        verificaPassword(utente, richiesta.passwordAttuale());

        if (passwordEncoder.matches(richiesta.nuovaPassword(), utente.getPassword())) {
            throw new OperazioneNonValidaException("La nuova password deve essere diversa da quella attuale");
        }
        utente.setPassword(passwordEncoder.encode(richiesta.nuovaPassword()));
        // Prima si scrive la modifica: la cancellazione di massa che segue svuota il contesto di JPA
        utenteRepository.saveAndFlush(utente);

        // Le sessioni aperte altrove devono rifare il login con la nuova password
        refreshTokenRepository.eliminaTuttiPerUtente(utenteId);
    }

    @Override
    @Transactional
    public void eliminaAccount(Long utenteId, ConfermaPasswordRequest richiesta) {
        verificaPassword(trovaUtente(utenteId), richiesta.password());
        utenteService.eliminaAccount(utenteId);
    }

    private AutenticazioneResponse emettiTokens(Utente utente) {
        String refreshGrezzo = UtilitaToken.generaCasuale();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UtilitaToken.calcolaHash(refreshGrezzo)); // nel database solo l'hash
        refreshToken.setDataScadenza(LocalDateTime.now().plusDays(jwtService.getRefreshTtlGiorni()));
        refreshToken.setUtente(utente);
        refreshTokenRepository.save(refreshToken);

        return new AutenticazioneResponse(
                jwtService.generaAccessToken(utente),
                refreshGrezzo,
                "Bearer",
                jwtService.getAccessTtlSecondi(),
                utenteMapper.aResponse(utente));
    }

    private Utente trovaUtente(Long utenteId) {
        return utenteRepository.findById(utenteId)
                .orElseThrow(() -> new RisorsaNonTrovataException("Utente non trovato"));
    }

    private void verificaPassword(Utente utente, String password) {
        if (!passwordEncoder.matches(password, utente.getPassword())) {
            throw new OperazioneNonValidaException("Password non corretta");
        }
    }
}