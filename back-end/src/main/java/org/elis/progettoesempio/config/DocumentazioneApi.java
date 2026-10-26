// config/DocumentazioneApi.java
package org.elis.progettoesempio.config;

import java.util.List;
import java.util.Map;

import org.elis.progettoesempio.security.UtenteCorrente;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * Tutta la documentazione OpenAPI in un posto solo: informazioni generali, autenticazione JWT, gruppi,
 * riepilogo di ogni endpoint e codici di errore comuni. I controller restano senza annotazioni di documentazione.
 * Quando si aggiunge un endpoint basta aggiungere una riga a RIEPILOGHI (un test controlla che non ne manchi nessuna).
 */
@Configuration
public class DocumentazioneApi {

    static final String SCHEMA_JWT = "token-jwt";

    static {
        // Il parametro @UtenteCorrente lo riempie il server dal token: non è un parametro dell'API
        SpringDocUtils.getConfig().addAnnotationsToIgnore(UtenteCorrente.class);
    }

    /** Il riepilogo di ogni endpoint, con indicato chi lo può usare. Chiave: "METODO percorso". */
    private static final Map<String, String> RIEPILOGHI = Map.ofEntries(
            // Autenticazione (pubblici)
            Map.entry("POST /api/auth/registrazione", "Registra un nuovo utente e crea il suo spazio personale"),
            Map.entry("POST /api/auth/login", "Accede con username o email e password"),
            Map.entry("POST /api/auth/refresh", "Ottiene nuovi token con il refresh token (monouso)"),
            Map.entry("POST /api/auth/logout", "Chiude la sessione revocando il refresh token"),

            // Profilo
            Map.entry("GET /api/utenti/me", "Profilo dell'utente autenticato"),
            Map.entry("PUT /api/utenti/me", "Modifica i dati del profilo"),
            Map.entry("PATCH /api/utenti/me/password", "Cambia la password (richiede quella attuale) e chiude le altre sessioni"),
            Map.entry("POST /api/utenti/me/eliminazione", "Elimina l'account (richiede la password)"),

            // Spazi di lavoro e membri
            Map.entry("GET /api/spazi-lavoro", "Elenca gli spazi di lavoro dell'utente, il personale per primo"),
            Map.entry("POST /api/spazi-lavoro", "Crea uno spazio aziendale: chi lo crea ne è il proprietario"),
            Map.entry("GET /api/spazi-lavoro/{spazioId}", "Dettaglio di uno spazio"),
            Map.entry("PUT /api/spazi-lavoro/{spazioId}", "Modifica nome e valuta (proprietario o amministratore)"),
            Map.entry("DELETE /api/spazi-lavoro/{spazioId}", "Elimina lo spazio con tutti i suoi dati (solo proprietario)"),
            Map.entry("GET /api/spazi-lavoro/{spazioId}/membri", "Elenca i membri dello spazio"),
            Map.entry("POST /api/spazi-lavoro/{spazioId}/membri", "Aggiunge un membro per email (proprietario o amministratore)"),
            Map.entry("PATCH /api/spazi-lavoro/{spazioId}/membri/{membroUtenteId}", "Cambia il ruolo di un membro (solo proprietario)"),
            Map.entry("DELETE /api/spazi-lavoro/{spazioId}/membri/{membroUtenteId}", "Rimuove un membro; ognuno può uscire dallo spazio, tranne il proprietario"),

            // Categorie
            Map.entry("GET /api/spazi-lavoro/{spazioId}/categorie", "Categorie dello spazio più quelle predefinite adatte al suo tipo"),
            Map.entry("POST /api/spazi-lavoro/{spazioId}/categorie", "Crea una categoria (proprietario o amministratore)"),
            Map.entry("PUT /api/spazi-lavoro/{spazioId}/categorie/{categoriaId}", "Modifica una categoria dello spazio (proprietario o amministratore)"),
            Map.entry("DELETE /api/spazi-lavoro/{spazioId}/categorie/{categoriaId}", "Elimina una categoria non usata (proprietario o amministratore)"),

            // Transazioni
            Map.entry("GET /api/spazi-lavoro/{spazioId}/transazioni", "Cerca le transazioni con filtri, ordinamento e paginazione"),
            Map.entry("POST /api/spazi-lavoro/{spazioId}/transazioni", "Inserisce una transazione (tutti tranne i lettori)"),
            Map.entry("GET /api/spazi-lavoro/{spazioId}/transazioni/{transazioneId}", "Dettaglio di una transazione"),
            Map.entry("PUT /api/spazi-lavoro/{spazioId}/transazioni/{transazioneId}", "Modifica una transazione (un membro solo le proprie)"),
            Map.entry("DELETE /api/spazi-lavoro/{spazioId}/transazioni/{transazioneId}", "Elimina una transazione (un membro solo le proprie)"),

            // Budget
            Map.entry("GET /api/spazi-lavoro/{spazioId}/budget", "Budget di un mese con speso, disponibile, percentuale e stato"),
            Map.entry("POST /api/spazi-lavoro/{spazioId}/budget", "Crea il budget mensile di una categoria di spesa (proprietario o amministratore)"),
            Map.entry("PATCH /api/spazi-lavoro/{spazioId}/budget/{budgetId}", "Modifica l'importo di un budget (proprietario o amministratore)"),
            Map.entry("DELETE /api/spazi-lavoro/{spazioId}/budget/{budgetId}", "Elimina un budget (proprietario o amministratore)"),

            // Obiettivi di risparmio
            Map.entry("GET /api/spazi-lavoro/{spazioId}/obiettivi", "Elenca gli obiettivi di risparmio"),
            Map.entry("POST /api/spazi-lavoro/{spazioId}/obiettivi", "Crea un obiettivo di risparmio (proprietario o amministratore)"),
            Map.entry("GET /api/spazi-lavoro/{spazioId}/obiettivi/{obiettivoId}", "Dettaglio di un obiettivo"),
            Map.entry("PUT /api/spazi-lavoro/{spazioId}/obiettivi/{obiettivoId}", "Modifica un obiettivo (proprietario o amministratore)"),
            Map.entry("DELETE /api/spazi-lavoro/{spazioId}/obiettivi/{obiettivoId}", "Elimina un obiettivo (proprietario o amministratore)"),
            Map.entry("PATCH /api/spazi-lavoro/{spazioId}/obiettivi/{obiettivoId}/versamenti", "Aggiunge denaro all'obiettivo (proprietario o amministratore)"),

            // Dipendenti e stipendi (solo spazi aziendali, solo chi gestisce)
            Map.entry("GET /api/spazi-lavoro/{spazioId}/dipendenti", "Elenca i dipendenti (proprietario o amministratore)"),
            Map.entry("POST /api/spazi-lavoro/{spazioId}/dipendenti", "Aggiunge un dipendente (proprietario o amministratore)"),
            Map.entry("GET /api/spazi-lavoro/{spazioId}/dipendenti/{dipendenteId}", "Dettaglio di un dipendente (proprietario o amministratore)"),
            Map.entry("PUT /api/spazi-lavoro/{spazioId}/dipendenti/{dipendenteId}", "Modifica o disattiva un dipendente (proprietario o amministratore)"),
            Map.entry("DELETE /api/spazi-lavoro/{spazioId}/dipendenti/{dipendenteId}", "Elimina un dipendente senza pagamenti (proprietario o amministratore)"),
            Map.entry("POST /api/spazi-lavoro/{spazioId}/dipendenti/{dipendenteId}/pagamenti-stipendio", "Registra lo stipendio di un mese: crea la transazione di uscita"),
            Map.entry("GET /api/spazi-lavoro/{spazioId}/dipendenti/{dipendenteId}/pagamenti-stipendio", "Storico dei pagamenti di stipendio del dipendente"),

            // Dashboard e report
            Map.entry("GET /api/spazi-lavoro/{spazioId}/dashboard/riepilogo", "Schede della dashboard per un mese"),
            Map.entry("GET /api/spazi-lavoro/{spazioId}/dashboard/trend", "Entrate, uscite e saldo per settimana o mese"),
            Map.entry("GET /api/spazi-lavoro/{spazioId}/dashboard/spese-per-categoria", "Distribuzione delle uscite per categoria"),
            Map.entry("GET /api/spazi-lavoro/{spazioId}/report/mensile", "Scarica il PDF del resoconto mensile (proprietario o amministratore)"),

            // Notifiche
            Map.entry("GET /api/notifiche", "Elenca le notifiche dell'utente"),
            Map.entry("GET /api/notifiche/conteggio-non-lette", "Numero di notifiche non lette"),
            Map.entry("PATCH /api/notifiche/{notificaId}/letta", "Segna una notifica come letta"),
            Map.entry("PATCH /api/notifiche/segna-tutte-come-lette", "Segna tutte le notifiche come lette"),
            Map.entry("DELETE /api/notifiche/{notificaId}", "Elimina una notifica"),

            // Notizie (pubblico)
            Map.entry("GET /api/notizie", "Ultime notizie di economia internazionale dai feed RSS delle testate"),

            // Amministrazione (solo ruolo ADMIN)
            Map.entry("GET /api/admin/utenti", "Cerca gli utenti della piattaforma (solo admin)"),
            Map.entry("PATCH /api/admin/utenti/{utenteId}/abilitazione", "Blocca o sblocca un utente (solo admin)"),
            Map.entry("GET /api/admin/categorie", "Elenca le categorie globali (solo admin)"),
            Map.entry("POST /api/admin/categorie", "Crea una categoria globale (solo admin)"),
            Map.entry("PUT /api/admin/categorie/{categoriaId}", "Modifica una categoria globale, tranne Stipendi (solo admin)"),
            Map.entry("DELETE /api/admin/categorie/{categoriaId}", "Elimina una categoria globale non usata, tranne Stipendi (solo admin)"),
            Map.entry("GET /api/admin/statistiche", "Conteggi generali della piattaforma, senza dati finanziari (solo admin)"));

    @Bean
    public OpenAPI informazioniApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Personal Finance API")
                        .version("1.0.0")
                        .description("""
                                API REST della piattaforma di gestione delle finanze personali e aziendali.

                                **Come autenticarsi:** esegui `POST /api/auth/login`, copia il valore di `accessToken` \
                                dalla risposta, premi *Authorize* in alto e incollalo (senza scrivere "Bearer"). \
                                Il token dura pochi minuti: per averne uno nuovo si usa `POST /api/auth/refresh`.

                                **Spazi di lavoro:** i dati finanziari appartengono a uno spazio (personale o aziendale). \
                                Chi non è membro di uno spazio riceve 404; chi è membro ma non ha il ruolo richiesto riceve 403."""))
                .addSecurityItem(new SecurityRequirement().addList(SCHEMA_JWT))
                .components(new Components().addSecuritySchemes(SCHEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token JWT ottenuto dal login")));
    }

    @Bean
    public OpenApiCustomizer completaLaDocumentazione() {
        return this::completa;
    }

    private void completa(OpenAPI openApi) {
        openApi.getComponents().addSchemas("ErroreResponse", schemaErrore());
        openApi.setTags(elencoTag());

        Schema<?> riferimentoErrore = new Schema<>().$ref("#/components/schemas/ErroreResponse");
        openApi.getPaths().forEach((percorso, voce) -> voce.readOperationsMap()
                .forEach((metodo, operazione) -> completaOperazione(percorso, metodo, operazione, riferimentoErrore)));
    }

    private void completaOperazione(String percorso, PathItem.HttpMethod metodo, Operation operazione,
            Schema<?> riferimentoErrore) {
        operazione.setTags(List.of(tagPerPercorso(percorso)));

        String riepilogo = RIEPILOGHI.get(metodo.name() + " " + percorso);
        if (riepilogo != null && operazione.getSummary() == null) {
            operazione.setSummary(riepilogo);
        }

        // Accesso, registrazione e notizie sono pubblici: niente lucchetto
        boolean notizie = percorso.equals("/api/notizie");
        if (percorso.startsWith("/api/auth/") || notizie) {
            operazione.setSecurity(List.of());
        }

        aggiungiRisposta(operazione, "400", "Richiesta non valida o dati non validi", riferimentoErrore);
        if (!notizie) {
            String non401 = percorso.startsWith("/api/auth/")
                    ? "Credenziali o refresh token non validi"
                    : "Non autenticato: token mancante, scaduto o non valido";
            aggiungiRisposta(operazione, "401", non401, riferimentoErrore);
            aggiungiRisposta(operazione, "403", "Permessi insufficienti per questa operazione", riferimentoErrore);
        }
        aggiungiRisposta(operazione, "404", "Risorsa non trovata", riferimentoErrore);
        if (metodo != PathItem.HttpMethod.GET) {
            aggiungiRisposta(operazione, "409", "Conflitto con i dati esistenti", riferimentoErrore);
        }
        aggiungiRisposta(operazione, "500", "Errore interno del server", riferimentoErrore);
    }

    private void aggiungiRisposta(Operation operazione, String codice, String descrizione, Schema<?> schema) {
        if (operazione.getResponses().containsKey(codice)) {
            return;
        }
        operazione.getResponses().addApiResponse(codice, new ApiResponse()
                .description(descrizione)
                .content(new Content().addMediaType("application/json", new MediaType().schema(schema))));
    }

    /** La struttura dell'errore standard (la stessa di ErroreResponse) */
    private Schema<?> schemaErrore() {
        Schema<?> campo = new ObjectSchema()
                .addProperty("campo", new StringSchema().example("importo"))
                .addProperty("messaggio", new StringSchema().example("L'importo deve essere maggiore di zero"));
        return new ObjectSchema()
                .description("Risposta standard di tutti gli errori")
                .addProperty("timestamp", new DateTimeSchema())
                .addProperty("status", new IntegerSchema().example(400))
                .addProperty("errore", new StringSchema().example("ERRORE_VALIDAZIONE"))
                .addProperty("messaggio", new StringSchema().example("Dati non validi"))
                .addProperty("percorso", new StringSchema().example("/api/spazi-lavoro/1/transazioni"))
                .addProperty("campi", new ArraySchema().items(campo)
                        .description("Solo per gli errori di validazione: i campi sbagliati"));
    }

    private List<Tag> elencoTag() {
        return List.of(
                new Tag().name("Autenticazione").description("Registrazione, accesso e sessioni (pubblici)"),
                new Tag().name("Profilo").description("Dati e password dell'utente autenticato"),
                new Tag().name("Spazi di lavoro").description("Spazi personali e aziendali"),
                new Tag().name("Membri").description("Team di uno spazio aziendale e ruoli"),
                new Tag().name("Categorie").description("Categorie di entrata e di uscita"),
                new Tag().name("Transazioni").description("Entrate e uscite, con ricerca e filtri"),
                new Tag().name("Budget").description("Budget mensili per categoria"),
                new Tag().name("Obiettivi di risparmio").description("Obiettivi e versamenti"),
                new Tag().name("Dipendenti e stipendi").description("Solo spazi aziendali, solo per chi gestisce"),
                new Tag().name("Dashboard").description("Riepiloghi e dati per i grafici"),
                new Tag().name("Report PDF").description("Resoconto mensile in PDF"),
                new Tag().name("Notifiche").description("Notifiche personali"),
                new Tag().name("Notizie").description("Notizie di economia internazionale (pubblico)"),
                new Tag().name("Amministrazione").description("Solo per gli amministratori della piattaforma"));
    }

    private String tagPerPercorso(String percorso) {
        if (percorso.startsWith("/api/auth/")) {
            return "Autenticazione";
        }
        if (percorso.startsWith("/api/utenti/")) {
            return "Profilo";
        }
        if (percorso.startsWith("/api/notifiche")) {
            return "Notifiche";
        }
        if (percorso.startsWith("/api/notizie")) {
            return "Notizie";
        }
        if (percorso.startsWith("/api/admin/")) {
            return "Amministrazione";
        }
        if (percorso.contains("/membri")) {
            return "Membri";
        }
        if (percorso.contains("/categorie")) {
            return "Categorie";
        }
        if (percorso.contains("/transazioni")) {
            return "Transazioni";
        }
        if (percorso.contains("/budget")) {
            return "Budget";
        }
        if (percorso.contains("/obiettivi")) {
            return "Obiettivi di risparmio";
        }
        if (percorso.contains("/dipendenti")) {
            return "Dipendenti e stipendi";
        }
        if (percorso.contains("/dashboard")) {
            return "Dashboard";
        }
        if (percorso.contains("/report")) {
            return "Report PDF";
        }
        return "Spazi di lavoro";
    }
}