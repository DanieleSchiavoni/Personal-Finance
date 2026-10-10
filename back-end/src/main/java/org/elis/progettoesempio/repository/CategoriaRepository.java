// CategoriaRepository.java
package org.elis.progettoesempio.repository;

import java.util.List;
import java.util.Optional;
import org.elis.progettoesempio.entity.Categoria;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /** Categorie dello spazio + globali adatte al suo tipo */
    @Query("""
        select c from Categoria c
        where c.spazioLavoro.id = :spazioLavoroId
           or (c.spazioLavoro is null and (c.tipoSpazio is null or c.tipoSpazio = :tipoSpazio))
        order by c.tipo, c.nome
        """)
    List<Categoria> trovaVisibili(@Param("spazioLavoroId") Long spazioLavoroId,
                                  @Param("tipoSpazio") TipoSpazio tipoSpazio);

    @Query("""
        select c from Categoria c
        where c.id = :id
          and (c.spazioLavoro.id = :spazioLavoroId
               or (c.spazioLavoro is null and (c.tipoSpazio is null or c.tipoSpazio = :tipoSpazio)))
        """)
    Optional<Categoria> trovaVisibilePerId(@Param("id") Long id,
                                           @Param("spazioLavoroId") Long spazioLavoroId,
                                           @Param("tipoSpazio") TipoSpazio tipoSpazio);

    Optional<Categoria> findByIdAndSpazioLavoroId(Long id, Long spazioLavoroId);

    /** Serve, ad esempio, per trovare la categoria globale "Stipendi" */
    Optional<Categoria> findBySpazioLavoroIsNullAndNomeAndTipo(String nome, TipoTransazione tipo);

    boolean existsBySpazioLavoroIdAndNomeIgnoreCaseAndTipo(Long spazioLavoroId, String nome, TipoTransazione tipo);

    boolean existsBySpazioLavoroIsNullAndNomeIgnoreCaseAndTipo(String nome, TipoTransazione tipo);
}