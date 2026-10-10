// MembroSpazioRepository.java
package org.elis.progettoesempio.repository;

import java.util.List;
import java.util.Optional;
import org.elis.progettoesempio.entity.MembroSpazio;
import org.elis.progettoesempio.entity.enumerazioni.RuoloSpazio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembroSpazioRepository extends JpaRepository<MembroSpazio, Long> {

    Optional<MembroSpazio> findBySpazioLavoroIdAndUtenteId(Long spazioLavoroId, Long utenteId);

    /** Spazi a cui appartiene un utente (per il selettore nella sidebar) */
    @Query("select m from MembroSpazio m join fetch m.spazioLavoro where m.utente.id = :utenteId")
    List<MembroSpazio> trovaPerUtenteConSpazio(@Param("utenteId") Long utenteId);

    /** Membri di uno spazio (per la pagina Team) */
    @Query("select m from MembroSpazio m join fetch m.utente where m.spazioLavoro.id = :spazioLavoroId")
    List<MembroSpazio> trovaPerSpazioConUtente(@Param("spazioLavoroId") Long spazioLavoroId);

    List<MembroSpazio> findByUtenteIdAndRuolo(Long utenteId, RuoloSpazio ruolo);

    long countBySpazioLavoroIdAndRuolo(Long spazioLavoroId, RuoloSpazio ruolo);
}