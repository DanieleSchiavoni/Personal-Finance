package org.elis.progettoesempio.repository.specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.elis.progettoesempio.dto.request.RicercaTransazioniRequest;
import org.elis.progettoesempio.entity.Transazione;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public final class TransazioneSpecification {

    private static final char ESCAPE = '!';

    private TransazioneSpecification() {
    }

    /**
     * @param nascondiStipendi true per MEMBRO e LETTORE: i pagamenti di stipendio non devono comparire
     */
    public static Specification<Transazione> conFiltri(Long spazioLavoroId,
            RicercaTransazioniRequest ricerca, boolean nascondiStipendi) {

        return (root, query, cb) -> {
            // Nella query di conteggio (paginazione) le fetch join non servono e non sono ammesse
            if (!Long.class.equals(query.getResultType())) {
                root.fetch("categoria", JoinType.INNER);
                root.fetch("creataDa", JoinType.LEFT);
            }

            List<Predicate> condizioni = new ArrayList<>();

            // Sempre presente: i dati sono solo quelli dello spazio richiesto
            condizioni.add(cb.equal(root.get("spazioLavoro").get("id"), spazioLavoroId));

            if (ricerca.dal() != null) {
                condizioni.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("dataTransazione"), ricerca.dal()));
            }
            if (ricerca.al() != null) {
                condizioni.add(cb.lessThanOrEqualTo(root.<LocalDate>get("dataTransazione"), ricerca.al()));
            }
            if (ricerca.categoriaId() != null) {
                condizioni.add(cb.equal(root.get("categoria").get("id"), ricerca.categoriaId()));
            }
            if (ricerca.tipo() != null) {
                condizioni.add(cb.equal(root.get("tipo"), ricerca.tipo()));
            }
            if (ricerca.metodoPagamento() != null) {
                condizioni.add(cb.equal(root.get("metodoPagamento"), ricerca.metodoPagamento()));
            }
            if (ricerca.importoMin() != null) {
                condizioni.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("importo"), ricerca.importoMin()));
            }
            if (ricerca.importoMax() != null) {
                condizioni.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("importo"), ricerca.importoMax()));
            }
            if (ricerca.testo() != null && !ricerca.testo().isBlank()) {
                String modello = "%" + proteggi(ricerca.testo().trim().toLowerCase(Locale.ROOT)) + "%";
                condizioni.add(cb.or(
                        cb.like(cb.lower(root.<String>get("descrizione")), modello, ESCAPE),
                        cb.like(cb.lower(root.<String>get("note")), modello, ESCAPE)));
            }
            if (nascondiStipendi) {
                condizioni.add(cb.isNull(root.get("dipendente")));
            }

            return cb.and(condizioni.toArray(new Predicate[0]));
        };
    }

    /** Fa trattare % e _ come caratteri normali nella ricerca testuale */
    private static String proteggi(String testo) {
        return testo
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}