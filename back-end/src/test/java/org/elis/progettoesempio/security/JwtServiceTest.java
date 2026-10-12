package org.elis.progettoesempio.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.elis.progettoesempio.entity.Utente;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SEGRETO = "segreto-di-prova-lungo-almeno-32-caratteri-0123456789";

    private Utente utenteConId(long id) {
        Utente utente = new Utente();
        utente.setId(id);
        return utente;
    }

    @Test
    void ilTokenContieneLIdDellUtente() {
        JwtService servizio = new JwtService(SEGRETO, 15, 7);

        String token = servizio.generaAccessToken(utenteConId(42L));

        assertThat(servizio.estraiUtenteId(token)).contains(42L);
    }

    @Test
    void unTokenModificatoVieneRifiutato() {
        JwtService servizio = new JwtService(SEGRETO, 15, 7);
        String[] parti = servizio.generaAccessToken(utenteConId(42L)).split("\\.");

        // Si altera il contenuto (payload) lasciando la firma originale
        String manomesso = parti[0] + "." + parti[1] + "x" + "." + parti[2];

        assertThat(servizio.estraiUtenteId(manomesso)).isEmpty();
    }

    @Test
    void unTokenFirmatoConUnAltroSegretoVieneRifiutato() {
        JwtService servizio = new JwtService(SEGRETO, 15, 7);
        JwtService altroServizio = new JwtService("un-altro-segreto-diverso-di-almeno-32-caratteri-99", 15, 7);

        String tokenAltrui = altroServizio.generaAccessToken(utenteConId(42L));

        assertThat(servizio.estraiUtenteId(tokenAltrui)).isEmpty();
    }

    @Test
    void unTokenScadutoVieneRifiutato() {
        // Durata negativa: il token nasce già scaduto
        JwtService servizio = new JwtService(SEGRETO, -1, 7);

        String token = servizio.generaAccessToken(utenteConId(42L));

        assertThat(servizio.estraiUtenteId(token)).isEmpty();
    }

    @Test
    void unSegretoTroppoCortoImpedisceLAvvio() {
        assertThatThrownBy(() -> new JwtService("corto", 15, 7))
                .isInstanceOf(IllegalStateException.class);
    }
}