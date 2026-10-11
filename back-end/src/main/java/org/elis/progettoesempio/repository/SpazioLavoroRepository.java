package org.elis.progettoesempio.repository;

import org.elis.progettoesempio.entity.SpazioLavoro;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpazioLavoroRepository extends JpaRepository<SpazioLavoro, Long> {

    long countByTipo(TipoSpazio tipo);
}