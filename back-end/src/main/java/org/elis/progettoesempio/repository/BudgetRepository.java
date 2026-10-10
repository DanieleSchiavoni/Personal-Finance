// BudgetRepository.java
package org.elis.progettoesempio.repository;

import java.util.List;
import java.util.Optional;
import org.elis.progettoesempio.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @Query("""
        select b from Budget b join fetch b.categoria
        where b.spazioLavoro.id = :spazioLavoroId
          and b.mese = :mese and b.anno = :anno
        """)
    List<Budget> trovaPerPeriodo(@Param("spazioLavoroId") Long spazioLavoroId,
                                 @Param("mese") int mese,
                                 @Param("anno") int anno);

    Optional<Budget> findByIdAndSpazioLavoroId(Long id, Long spazioLavoroId);

    boolean existsBySpazioLavoroIdAndCategoriaIdAndMeseAndAnno(
            Long spazioLavoroId, Long categoriaId, int mese, int anno);

    boolean existsByCategoriaId(Long categoriaId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Budget b where b.spazioLavoro.id = :spazioLavoroId")
    void eliminaTuttiPerSpazioLavoro(@Param("spazioLavoroId") Long spazioLavoroId);
}