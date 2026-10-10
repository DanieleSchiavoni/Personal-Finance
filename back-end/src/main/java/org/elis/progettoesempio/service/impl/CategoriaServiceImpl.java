// service/impl/CategoriaServiceImpl.java
package org.elis.progettoesempio.service.impl;

import java.util.List;

import org.elis.progettoesempio.dto.request.CategoriaRequest;
import org.elis.progettoesempio.dto.response.CategoriaResponse;
import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.elis.progettoesempio.exception.ConflittoException;
import org.elis.progettoesempio.exception.RisorsaNonTrovataException;
import org.elis.progettoesempio.mapper.CategoriaMapper;
import org.elis.progettoesempio.repository.BudgetRepository;
import org.elis.progettoesempio.repository.CategoriaRepository;
import org.elis.progettoesempio.repository.TransazioneRepository;
import org.elis.progettoesempio.service.AccessoSpazioService;
import org.elis.progettoesempio.service.CategoriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final TransazioneRepository transazioneRepository;
    private final BudgetRepository budgetRepository;
    private final AccessoSpazioService accessoSpazioService;
    private final CategoriaMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> elenca(Long spazioLavoroId, Long utenteId) {
        MembroSpazio membro = accessoSpazioService.verificaMembro(spazioLavoroId, utenteId);

        return categoriaRepository
                .trovaVisibili(spazioLavoroId, membro.getSpazioLavoro().getTipo())
                .stream()
                .map(mapper::aResponse)
                .toList();
    }

    @Override
    @Transactional
    public CategoriaResponse crea(Long spazioLavoroId, Long utenteId, CategoriaRequest richiesta) {
        MembroSpazio membro = accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        verificaNomeLibero(spazioLavoroId, richiesta.nome().trim(), richiesta.tipo());

        Categoria categoria = new Categoria();
        categoria.setSpazioLavoro(membro.getSpazioLavoro());
        copiaDati(categoria, richiesta);
        return mapper.aResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaResponse aggiorna(Long spazioLavoroId, Long utenteId, Long categoriaId,
            CategoriaRequest richiesta) {
        accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);

        // Cerca solo tra le categorie dello spazio: quelle globali non risultano modificabili (404)
        Categoria categoria = trovaCategoriaDelloSpazio(categoriaId, spazioLavoroId);

        String nuovoNome = richiesta.nome().trim();
        boolean cambiaTipo = categoria.getTipo() != richiesta.tipo();
        boolean cambiaNome = !categoria.getNome().equalsIgnoreCase(nuovoNome);

        if (cambiaTipo && inUso(categoriaId)) {
            throw new ConflittoException("Non puoi cambiare il tipo di una categoria già utilizzata");
        }
        if (cambiaTipo || cambiaNome) {
            verificaNomeLibero(spazioLavoroId, nuovoNome, richiesta.tipo());
        }

        copiaDati(categoria, richiesta);
        return mapper.aResponse(categoria);
    }

    @Override
    @Transactional
    public void elimina(Long spazioLavoroId, Long utenteId, Long categoriaId) {
        accessoSpazioService.verificaPuoGestire(spazioLavoroId, utenteId);
        Categoria categoria = trovaCategoriaDelloSpazio(categoriaId, spazioLavoroId);

        if (inUso(categoriaId)) {
            throw new ConflittoException("La categoria è in uso (transazioni o budget) e non può essere eliminata");
        }
        categoriaRepository.delete(categoria);
    }

    private Categoria trovaCategoriaDelloSpazio(Long categoriaId, Long spazioLavoroId) {
        return categoriaRepository.findByIdAndSpazioLavoroId(categoriaId, spazioLavoroId)
                .orElseThrow(() -> new RisorsaNonTrovataException("Categoria non trovata"));
    }

    private boolean inUso(Long categoriaId) {
        return transazioneRepository.existsByCategoriaId(categoriaId)
                || budgetRepository.existsByCategoriaId(categoriaId);
    }

    /** Il nome deve essere unico (a parità di tipo) sia nello spazio sia tra le categorie globali */
    private void verificaNomeLibero(Long spazioLavoroId, String nome, TipoTransazione tipo) {
        boolean giaPresente =
                categoriaRepository.existsBySpazioLavoroIdAndNomeIgnoreCaseAndTipo(spazioLavoroId, nome, tipo)
                || categoriaRepository.existsBySpazioLavoroIsNullAndNomeIgnoreCaseAndTipo(nome, tipo);
        if (giaPresente) {
            throw new ConflittoException("Esiste già una categoria con questo nome");
        }
    }

    private void copiaDati(Categoria categoria, CategoriaRequest richiesta) {
        categoria.setNome(richiesta.nome().trim());
        categoria.setTipo(richiesta.tipo());
        categoria.setColore(richiesta.colore());
        categoria.setDescrizione(richiesta.descrizione());
    }
}