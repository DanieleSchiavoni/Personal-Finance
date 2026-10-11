package org.elis.progettoesempio.repository;

import java.util.Optional;

import org.elis.progettoesempio.entity.Utente;
import org.elis.progettoesempio.entity.enumerazioni.RuoloUtente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UtenteRepository extends JpaRepository<Utente, Long> {

    Optional<Utente> findByUsername(String username);

    Optional<Utente> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByRuolo(RuoloUtente ruolo);

    long countByAbilitato(boolean abilitato);

    /** Ricerca per admin. Il modello è già nella forma %testo% in minuscolo. */
    @Query("""
        select u from Utente u
        where lower(u.username) like :modello or lower(u.email) like :modello
           or lower(u.nome) like :modello or lower(u.cognome) like :modello
        """)
    Page<Utente> cerca(@Param("modello") String modello, Pageable pageable);
}