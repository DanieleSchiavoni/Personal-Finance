// NotificaRepository.java
package org.elis.progettoesempio.repository;

import java.util.List;
import java.util.Optional;
import org.elis.progettoesempio.entity.Notifica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificaRepository extends JpaRepository<Notifica, Long> {

    List<Notifica> findByUtenteIdOrderByDataCreazioneDesc(Long utenteId);

    Optional<Notifica> findByIdAndUtenteId(Long id, Long utenteId);

    long countByUtenteIdAndLettaFalse(Long utenteId);

    @Modifying(clearAutomatically = true)
    @Query("update Notifica n set n.letta = true where n.utente.id = :utenteId and n.letta = false")
    int segnaTutteComeLette(@Param("utenteId") Long utenteId);
}