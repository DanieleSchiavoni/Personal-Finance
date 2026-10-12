package org.elis.progettoesempio.config;

import java.util.Locale;

import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.entity.enumerazioni.RuoloUtente;
import org.elis.progettoesempio.repository.UtenteRepository;
import org.elis.progettoesempio.service.SpazioLavoroService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test") // <-- Aggiungi questa riga per escluderlo durante l'esecuzione dei test
public class InizializzatoreAdmin implements ApplicationRunner {

    private final UtenteRepository utenteRepository;
    private final SpazioLavoroService spazioLavoroService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:}")
    private String username;
    @Value("${app.admin.email:}")
    private String email;
    @Value("${app.admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments argomenti) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            return; // variabili non impostate: niente da fare
        }
        if (utenteRepository.countByRuolo(RuoloUtente.ADMIN) > 0) {
            return; // un amministratore esiste già
        }

        String emailNormalizzata = email.trim().toLowerCase(Locale.ROOT);
        if (password.length() < 8
                || utenteRepository.existsByUsername(username.trim())
                || utenteRepository.existsByEmail(emailNormalizzata)) {
            log.warn("Amministratore iniziale non creato: password troppo corta (minimo 8) o username/email già in uso");
            return;
        }

        Utente admin = new Utente();
        admin.setUsername(username.trim());
        admin.setEmail(emailNormalizzata);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setNome("Amministratore");
        admin.setCognome("Piattaforma");
        admin.setRuolo(RuoloUtente.ADMIN);
        utenteRepository.save(admin);
        spazioLavoroService.creaSpazioPersonale(admin);

        log.info("Creato l'account amministratore '{}'", admin.getUsername());
    }
}