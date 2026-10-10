// ObiettivoRisparmioRepository.java
package org.elis.progettoesempio.repository;

import java.util.List;
import java.util.Optional;
import org.elis.progettoesempio.entity.ObiettivoRisparmio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObiettivoRisparmioRepository extends JpaRepository<ObiettivoRisparmio, Long> {
    List<ObiettivoRisparmio> findBySpazioLavoroIdOrderByScadenzaAsc(Long spazioLavoroId);
    Optional<ObiettivoRisparmio> findByIdAndSpazioLavoroId(Long id, Long spazioLavoroId);
}