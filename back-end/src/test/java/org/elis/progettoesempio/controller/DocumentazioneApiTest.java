// src/test/java/org/elis/progettoesempio/controller/DocumentazioneApiTest.java
package org.elis.progettoesempio.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * La documentazione OpenAPI è pubblica e generata dal codice: questi test verificano che sia completa
 * e coerente, così non si dimentica nessun endpoint quando se ne aggiungono di nuovi.
 */
class DocumentazioneApiTest extends ControllerTestBase {

    private static final String PERCORSO_TRANSAZIONI = "/paths/~1api~1spazi-lavoro~1{spazioId}~1transazioni";

    private JsonNode documentazione;

    @BeforeEach
    void leggiLaDocumentazione() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        documentazione = objectMapper.readTree(json);
    }

    @Test
    void laDocumentazioneEPubblicaEUsaOpenApi3() {
        assertThat(documentazione.get("openapi").asText()).startsWith("3.");
        assertThat(documentazione.at("/info/title").asText()).isEqualTo("Personal Finance API");
    }

    @Test
    void lAutenticazioneJwtEDocumentataEApplicataATutto() {
        JsonNode schema = documentazione.at("/components/securitySchemes/token-jwt");

        assertThat(schema.get("type").asText()).isEqualTo("http");
        assertThat(schema.get("scheme").asText()).isEqualTo("bearer");
        assertThat(schema.get("bearerFormat").asText()).isEqualTo("JWT");
        assertThat(documentazione.at("/security/0").has("token-jwt")).isTrue();
    }

    @Test
    void ognunaOperazioneHaUnRiepilogoEUnGruppo() {
        List<String> senzaRiepilogo = new ArrayList<>();
        List<String> senzaGruppo = new ArrayList<>();

        perOgniOperazione((nome, operazione) -> {
            if (!operazione.hasNonNull("summary") || operazione.get("summary").asText().isBlank()) {
                senzaRiepilogo.add(nome);
            }
            if (!operazione.has("tags") || operazione.get("tags").isEmpty()) {
                senzaGruppo.add(nome);
            }
        });

        // Se l'elenco non è vuoto, ha dimenticato una riga in RIEPILOGHI: il messaggio dice quale endpoint
        assertThat(senzaRiepilogo).as("operazioni senza riepilogo").isEmpty();
        assertThat(senzaGruppo).as("operazioni senza gruppo").isEmpty();
    }

    @Test
    void gliEndpointPubbliciNonRichiedonoAutenticazione() {
        assertThat(documentazione.at("/paths/~1api~1auth~1login/post/security").isArray()).isTrue();
        assertThat(documentazione.at("/paths/~1api~1auth~1login/post/security")).isEmpty();
        assertThat(documentazione.at("/paths/~1api~1notizie/get/security")).isEmpty();
    }

    @Test
    void ilParametroDellUtenteCorrenteNonEMostrato() {
        List<String> trovati = new ArrayList<>();

        perOgniOperazione((nome, operazione) -> {
            for (JsonNode parametro : operazione.path("parameters")) {
                String nomeParametro = parametro.get("name").asText();
                // Un parametro "utenteId" nel percorso è legittimo (ad esempio l'utente da bloccare):
                // quelli nascosti, se comparissero, sarebbero parametri di query
                boolean nelPercorso = "path".equals(parametro.path("in").asText());
                if (!nelPercorso && (nomeParametro.equals("utenteId") || nomeParametro.equals("adminId"))) {
                    trovati.add(nome + " -> " + nomeParametro);
                }
            }
        });

        assertThat(trovati).isEmpty();
    }
    @Test
    void laRicercaDelleTransazioniDocumentaIFiltriComeParametri() {
        List<String> nomi = new ArrayList<>();
        documentazione.at(PERCORSO_TRANSAZIONI + "/get/parameters")
                .forEach(parametro -> nomi.add(parametro.get("name").asText()));

        assertThat(nomi).contains("spazioId", "tipo", "dal", "al", "importoMin", "pagina", "dimensione", "ordinaPer");
    }

    @Test
    void leOperazioniDocumentanoICodiciDiErrore() {
        JsonNode risposte = documentazione.at(PERCORSO_TRANSAZIONI + "/post/responses");

        for (String codice : List.of("400", "401", "403", "404", "409", "500")) {
            assertThat(risposte.has(codice)).as("risposta " + codice).isTrue();
        }
    }

    @Test
    void loSchemaDellErroreStandardEDocumentato() {
        JsonNode proprieta = documentazione.at("/components/schemas/ErroreResponse/properties");

        assertThat(proprieta.has("errore")).isTrue();
        assertThat(proprieta.has("messaggio")).isTrue();
        assertThat(proprieta.has("campi")).isTrue();
    }

    @Test
    void leRichiesteMostranoGliSchemiDeiDtoConIVincoli() {
        String riferimento = documentazione
                .at(PERCORSO_TRANSAZIONI + "/post/requestBody/content/application~1json/schema/$ref").asText();
        assertThat(riferimento).endsWith("TransazioneRequest");

        List<String> obbligatori = new ArrayList<>();
        documentazione.at("/components/schemas/TransazioneRequest/required")
                .forEach(campo -> obbligatori.add(campo.asText()));
        assertThat(obbligatori).contains("importo", "descrizione", "data", "tipo", "categoriaId");
    }

    @Test
    void ilPdfDelResocontoDichiaraIlTipoDiContenutoCorretto() {
        JsonNode contenuto = documentazione
                .at("/paths/~1api~1spazi-lavoro~1{spazioId}~1report~1mensile/get/responses/200/content/application~1pdf");

        assertThat(contenuto.isMissingNode()).isFalse();
    }

    @Test
    void laPaginaSwaggerUiEPubblica() throws Exception {
        // Senza token la pagina non risponde 401: rimanda alla pagina vera dell'interfaccia
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    }

    private void perOgniOperazione(BiConsumer<String, JsonNode> azione) {
        documentazione.get("paths").fields().forEachRemaining(voce -> {
            for (String metodo : List.of("get", "post", "put", "patch", "delete")) {
                JsonNode operazione = voce.getValue().get(metodo);
                if (operazione != null) {
                    azione.accept(metodo.toUpperCase() + " " + voce.getKey(), operazione);
                }
            }
        });
    }
}