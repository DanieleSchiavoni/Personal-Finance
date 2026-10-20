// controller/AutorizzazioneRuoliTest.java
package org.elis.progettoesempio.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;

import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.RuoloSpazio;
import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.Utente;

import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class AutorizzazioneRuoliTest extends ControllerTestBase {

    private record Operazione(HttpMethod metodo, String risorsa, String corpo) {
    }

    private SpazioLavoro spazio;
    private Utente membro;
    private Utente lettore;
    private Categoria categoria;

    /** Le operazioni riservate a chi gestisce lo spazio (PROPRIETARIO e AMMINISTRATORE) */
    static Stream<Arguments> operazioniVietate() {
        List<Operazione> operazioni = List.of(
                new Operazione(HttpMethod.POST, "/categorie",
                        "{\"nome\":\"Prova\",\"tipo\":\"USCITA\",\"colore\":\"#112233\",\"descrizione\":null}"),
                new Operazione(HttpMethod.POST, "/budget",
                        "{\"categoriaId\":1,\"importo\":100,\"mese\":10,\"anno\":2026}"),
                new Operazione(HttpMethod.POST, "/obiettivi",
                        "{\"nome\":\"Prova\",\"descrizione\":null,\"importoObiettivo\":100,\"scadenza\":null}"),
                new Operazione(HttpMethod.POST, "/dipendenti",
                        "{\"nome\":\"Anna\",\"cognome\":\"Bianchi\",\"mansione\":null,\"stipendioMensile\":100,"
                                + "\"dataAssunzione\":null,\"attivo\":true}"),
                new Operazione(HttpMethod.GET, "/dipendenti", null),
                new Operazione(HttpMethod.POST, "/membri", "{\"email\":\"qualcuno@test.it\",\"ruolo\":\"MEMBRO\"}"),
                new Operazione(HttpMethod.PUT, "", "{\"nome\":\"Nuovo nome\",\"valuta\":\"EUR\"}"),
                new Operazione(HttpMethod.DELETE, "", null),
                new Operazione(HttpMethod.GET, "/report/mensile", null));

        return Stream.of(RuoloSpazio.MEMBRO, RuoloSpazio.LETTORE)
                .flatMap(ruolo -> operazioni.stream()
                        .map(operazione -> Arguments.of(ruolo, operazione.metodo(), operazione.risorsa(),
                                operazione.corpo())));
    }

    @BeforeEach
    void preparaDati() {
        spazio = creaSpazio(TipoSpazio.AZIENDALE);
        Utente proprietario = creaUtente("proprietario");
        membro = creaUtente("membro");
        lettore = creaUtente("lettore");
        aggiungiMembro(spazio, proprietario, RuoloSpazio.PROPRIETARIO);
        aggiungiMembro(spazio, membro, RuoloSpazio.MEMBRO);
        aggiungiMembro(spazio, lettore, RuoloSpazio.LETTORE);
        categoria = creaCategoria(spazio, "Ristoranti", TipoTransazione.USCITA);
    }

    @ParameterizedTest(name = "{0} non puo eseguire {1} {2}")
    @MethodSource("operazioniVietate")
    void ImembriNonGestoriRicevono403(RuoloSpazio ruolo, HttpMethod metodo, String risorsa, String corpo)
            throws Exception {
        Utente utente = ruolo == RuoloSpazio.MEMBRO ? membro : lettore;
        MockHttpServletRequestBuilder richiesta = request(metodo, "/api/spazi-lavoro/" + spazio.getId() + risorsa)
                .header(AUTORIZZAZIONE, bearer(utente));
        if (corpo != null) {
            richiesta.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }

        mockMvc.perform(richiesta)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errore").value("ACCESSO_NEGATO"));
    }

    @Test
    void ilLettoreNonPuoInserireTransazioniIlMembroSi() throws Exception {
        String corpo = "{\"importo\":10.00,\"descrizione\":\"Caffe\",\"data\":\"2026-10-06\",\"tipo\":\"USCITA\","
                + "\"metodoPagamento\":\"CARTA\",\"categoriaId\":" + categoria.getId() + ",\"note\":null}";
        String percorso = "/api/spazi-lavoro/" + spazio.getId() + "/transazioni";

        mockMvc.perform(request(HttpMethod.POST, percorso).header(AUTORIZZAZIONE, bearer(lettore))
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());
        mockMvc.perform(request(HttpMethod.POST, percorso).header(AUTORIZZAZIONE, bearer(membro))
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());
    }
}