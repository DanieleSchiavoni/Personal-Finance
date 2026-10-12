package org.elis.progettoesempio.controller;

import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Utente;

import org.elis.progettoesempio.entity.enumerazioni.RuoloUtente;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.repository.CategoriaRepository;
import org.elis.progettoesempio.repository.MembroSpazioRepository;
import org.elis.progettoesempio.repository.SpazioLavoroRepository;
import org.elis.progettoesempio.repository.TransazioneRepository;
import org.elis.progettoesempio.repository.UtenteRepository;
import org.elis.progettoesempio.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Base dei test sui controller: applicazione completa, database H2, ogni test annullato alla fine */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class ControllerTestBase {

    protected static final String AUTORIZZAZIONE = HttpHeaders.AUTHORIZATION;

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected JwtService jwtService;
    @Autowired
    protected UtenteRepository utenteRepository;
    @Autowired
    protected SpazioLavoroRepository spazioLavoroRepository;
    @Autowired
    protected MembroSpazioRepository membroSpazioRepository;
    @Autowired
    protected CategoriaRepository categoriaRepository;
    @Autowired
    protected TransazioneRepository transazioneRepository;

    protected String json(Object oggetto) throws JsonProcessingException {
        return objectMapper.writeValueAsString(oggetto);
    }

    /** Valore dell'intestazione Authorization con un token di accesso valido per l'utente */
    protected String bearer(Utente utente) {
        return "Bearer " + jwtService.generaAccessToken(utente);
    }

    protected Utente creaUtente(String nome) {
        return creaUtente(nome, RuoloUtente.UTENTE);
    }

    protected Utente creaUtente(String nome, RuoloUtente ruolo) {
        Utente utente = new Utente();
        utente.setUsername(nome);
        utente.setEmail(nome + "@test.it");
        utente.setPassword("password-di-prova");
        utente.setNome(nome);
        utente.setCognome("Test");
        utente.setRuolo(ruolo);
        return utenteRepository.save(utente);
    }

    protected SpazioLavoro creaSpazio(TipoSpazio tipo) {
        SpazioLavoro spazio = new SpazioLavoro();
        spazio.setNome("Spazio di prova");
        spazio.setTipo(tipo);
        return spazioLavoroRepository.save(spazio);
    }

    protected void aggiungiMembro(SpazioLavoro spazio, Utente utente, RuoloSpazio ruolo) {
        MembroSpazio appartenenza = new MembroSpazio();
        appartenenza.setSpazioLavoro(spazio);
        appartenenza.setUtente(utente);
        appartenenza.setRuolo(ruolo);
        membroSpazioRepository.save(appartenenza);
    }

    protected Categoria creaCategoria(SpazioLavoro spazio, String nome, TipoTransazione tipo) {
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setTipo(tipo);
        categoria.setSpazioLavoro(spazio);
        return categoriaRepository.save(categoria);
    }
}