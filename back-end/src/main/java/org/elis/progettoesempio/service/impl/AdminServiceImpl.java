// service/impl/AdminServiceImpl.java
package org.elis.progettoesempio.service.impl;

import java.util.List;
import java.util.Locale;

import org.elis.progettoesempio.dto.request.CategoriaGlobaleRequest;
import org.elis.progettoesempio.dto.request.RicercaUtentiRequest;
import org.elis.progettoesempio.dto.response.CategoriaGlobaleResponse;
import org.elis.progettoesempio.dto.response.PaginaResponse;
import org.elis.progettoesempio.dto.response.StatisticheGeneraliResponse;
import org.elis.progettoesempio.dto.response.UtenteResponse;
import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.entity.enumerazioni.RuoloUtente;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.exception.AccessoNegatoException;
import org.elis.progettoesempio.exception.ConflittoException;
import org.elis.progettoesempio.exception.OperazioneNonValidaException;
import org.elis.progettoesempio.exception.RisorsaNonTrovataException;
import org.elis.progettoesempio.mapper.CategoriaGlobaleMapper;
import org.elis.progettoesempio.mapper.UtenteMapper;
import org.elis.progettoesempio.repository.BudgetRepository;
import org.elis.progettoesempio.repository.CategoriaRepository;
import org.elis.progettoesempio.repository.RefreshTokenRepository;
import org.elis.progettoesempio.repository.SpazioLavoroRepository;
import org.elis.progettoesempio.repository.TransazioneRepository;
import org.elis.progettoesempio.repository.UtenteRepository;
import org.elis.progettoesempio.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final int DIMENSIONE_PREDEFINITA = 20;

    /** I pagamenti degli stipendi cercano questa categoria per nome: non si può toccare */
    private static final String NOME_CATEGORIA_PROTETTA = "Stipendi";

    private final UtenteRepository utenteRepository;
    private final SpazioLavoroRepository spazioLavoroRepository;
    private final CategoriaRepository categoriaRepository;
    private final TransazioneRepository transazioneRepository;
    private final BudgetRepository budgetRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UtenteMapper utenteMapper;
    private final CategoriaGlobaleMapper categoriaMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<UtenteResponse> elencaUtenti(Long adminId, RicercaUtentiRequest ricerca) {
        verificaAdmin(adminId);

        String testo = ricerca.testo() == null ? "" : ricerca.testo().trim().toLowerCase(Locale.ROOT);
        int pagina = ricerca.pagina() != null ? ricerca.pagina() : 0;
        int dimensione = ricerca.dimensione() != null ? ricerca.dimensione() : DIMENSIONE_PREDEFINITA;

        Page<Utente> risultato = utenteRepository.cerca(
                "%" + testo + "%", PageRequest.of(pagina, dimensione, Sort.by("username")));
        return PaginaResponse.da(risultato.map(utenteMapper::aResponse));
    }

    @Override
    @Transactional
    public UtenteResponse impostaAbilitazione(Long adminId, Long utenteId, boolean abilitato) {
        verificaAdmin(adminId);
        if (adminId.equals(utenteId)) {
            throw new OperazioneNonValidaException("Non puoi bloccare o sbloccare il tuo stesso account");
        }

        // Prima la cancellazione di massa (svuota il contesto di JPA), poi si legge e si modifica l'utente
        if (!abilitato) {
            refreshTokenRepository.eliminaTuttiPerUtente(utenteId);
        }
        Utente utente = utenteRepository.findById(utenteId)
                .orElseThrow(() -> new RisorsaNonTrovataException("Utente non trovato"));
        utente.setAbilitato(abilitato);
        return utenteMapper.aResponse(utente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaGlobaleResponse> elencaCategorieGlobali(Long adminId) {
        verificaAdmin(adminId);
        return categoriaRepository.findBySpazioLavoroIsNullOrderByTipoAscNomeAsc().stream()
                .map(categoriaMapper::aResponse)
                .toList();
    }

    @Override
    @Transactional
    public CategoriaGlobaleResponse creaCategoriaGlobale(Long adminId, CategoriaGlobaleRequest richiesta) {
        verificaAdmin(adminId);
        verificaNomeLibero(richiesta.nome().trim(), richiesta.tipo());

        Categoria categoria = new Categoria(); // spazioLavoro vuoto = categoria globale
        copiaDati(categoria, richiesta);
        return categoriaMapper.aResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaGlobaleResponse aggiornaCategoriaGlobale(Long adminId, Long categoriaId,
            CategoriaGlobaleRequest richiesta) {
        verificaAdmin(adminId);
        Categoria categoria = trovaCategoriaGlobale(categoriaId);
        verificaNonProtetta(categoria);

        String nuovoNome = richiesta.nome().trim();
        boolean cambiaTipo = categoria.getTipo() != richiesta.tipo();
        boolean cambiaNome = !categoria.getNome().equalsIgnoreCase(nuovoNome);

        if (cambiaTipo && inUso(categoriaId)) {
            throw new ConflittoException("Non puoi cambiare il tipo di una categoria già utilizzata");
        }
        if (cambiaTipo || cambiaNome) {
            verificaNomeLibero(nuovoNome, richiesta.tipo());
        }

        copiaDati(categoria, richiesta);
        return categoriaMapper.aResponse(categoria);
    }

    @Override
    @Transactional
    public void eliminaCategoriaGlobale(Long adminId, Long categoriaId) {
        verificaAdmin(adminId);
        Categoria categoria = trovaCategoriaGlobale(categoriaId);
        verificaNonProtetta(categoria);

        if (inUso(categoriaId)) {
            throw new ConflittoException("La categoria è in uso (transazioni o budget) e non può essere eliminata");
        }
        categoriaRepository.delete(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    public StatisticheGeneraliResponse statistiche(Long adminId) {
        verificaAdmin(adminId);

        long utentiAbilitati = utenteRepository.countByAbilitato(true);
        long utentiBloccati = utenteRepository.countByAbilitato(false);

        return new StatisticheGeneraliResponse(
                utentiAbilitati + utentiBloccati,
                utentiAbilitati,
                utentiBloccati,
                utenteRepository.countByRuolo(RuoloUtente.ADMIN),
                spazioLavoroRepository.countByTipo(TipoSpazio.PERSONALE),
                spazioLavoroRepository.countByTipo(TipoSpazio.AZIENDALE),
                transazioneRepository.count(),
                categoriaRepository.countBySpazioLavoroIsNull());
    }

    /** Controllo di sicurezza del service, oltre a quello degli endpoint */
    private void verificaAdmin(Long adminId) {
        Utente admin = utenteRepository.findById(adminId)
                .orElseThrow(() -> new AccessoNegatoException("Operazione riservata agli amministratori"));
        if (admin.getRuolo() != RuoloUtente.ADMIN || !admin.isAbilitato()) {
            throw new AccessoNegatoException("Operazione riservata agli amministratori");
        }
    }

    private Categoria trovaCategoriaGlobale(Long categoriaId) {
        return categoriaRepository.findByIdAndSpazioLavoroIsNull(categoriaId)
                .orElseThrow(() -> new RisorsaNonTrovataException("Categoria globale non trovata"));
    }

    private void verificaNonProtetta(Categoria categoria) {
        if (NOME_CATEGORIA_PROTETTA.equalsIgnoreCase(categoria.getNome())
                && categoria.getTipo() == TipoTransazione.USCITA) {
            throw new OperazioneNonValidaException(
                    "La categoria di sistema 'Stipendi' non può essere modificata né eliminata");
        }
    }

    private boolean inUso(Long categoriaId) {
        return transazioneRepository.existsByCategoriaId(categoriaId)
                || budgetRepository.existsByCategoriaId(categoriaId);
    }

    private void verificaNomeLibero(String nome, TipoTransazione tipo) {
        if (categoriaRepository.existsBySpazioLavoroIsNullAndNomeIgnoreCaseAndTipo(nome, tipo)) {
            throw new ConflittoException("Esiste già una categoria globale con questo nome");
        }
    }

    private void copiaDati(Categoria categoria, CategoriaGlobaleRequest richiesta) {
        categoria.setNome(richiesta.nome().trim());
        categoria.setTipo(richiesta.tipo());
        categoria.setColore(richiesta.colore());
        categoria.setDescrizione(richiesta.descrizione());
        categoria.setTipoSpazio(richiesta.tipoSpazio());
    }
}