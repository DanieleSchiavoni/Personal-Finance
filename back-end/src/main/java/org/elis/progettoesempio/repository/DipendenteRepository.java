// DipendenteRepository.java
package org.elis.progettoesempio.repository;

import java.util.List;
import java.util.Optional;
import org.elis.progettoesempio.entity.Dipendente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DipendenteRepository extends JpaRepository<Dipendente, Long> {
    List<Dipendente> findBySpazioLavoroIdOrderByCognomeAscNomeAsc(Long spazioLavoroId);
    Optional<Dipendente> findByIdAndSpazioLavoroId(Long id, Long spazioLavoroId);
}