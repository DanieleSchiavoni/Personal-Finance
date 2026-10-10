// TransazioneRepository.java
package org.elis.progettoesempio.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.elis.progettoesempio.entity.Transazione;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// JpaSpecificationExecutor serve per i filtri dinamici della ricerca (Fase 4)
public interface TransazioneRepository
        extends JpaRepository<Transazione, Long>, JpaSpecificationExecutor<Transazione> {

    Optional<Transazione> findByIdAndSpazioLavoroId(Long id, Long spazioLavoroId);

    List<Transazione> findTop5BySpazioLavoroIdOrderByDataTransazioneDescIdDesc(Long spazioLavoroId);

    long countBySpazioLavoroId(Long spazioLavoroId);

    boolean existsByCategoriaId(Long categoriaId);

    boolean existsByDipendenteIdAndAnnoStipendioAndMeseStipendio(Long dipendenteId, Integer anno, Integer mese);

    /** Somma totale per tipo (serve al saldo complessivo) */
    @Query("""
        select coalesce(sum(t.importo), 0) from Transazione t
        where t.spazioLavoro.id = :spazioLavoroId and t.tipo = :tipo
        """)
    BigDecimal sommaPerTipo(@Param("spazioLavoroId") Long spazioLavoroId,
                            @Param("tipo") TipoTransazione tipo);

    @Query("""
        select coalesce(sum(t.importo), 0) from Transazione t
        where t.spazioLavoro.id = :spazioLavoroId and t.tipo = :tipo
          and t.dataTransazione between :dal and :al
        """)
    BigDecimal sommaPerTipoNelPeriodo(@Param("spazioLavoroId") Long spazioLavoroId,
                                      @Param("tipo") TipoTransazione tipo,
                                      @Param("dal") LocalDate dal,
                                      @Param("al") LocalDate al);

    /** Speso per una categoria in un periodo: serve al calcolo dei budget */
    @Query("""
        select coalesce(sum(t.importo), 0) from Transazione t
        where t.spazioLavoro.id = :spazioLavoroId and t.categoria.id = :categoriaId
          and t.tipo = org.elis.progettoesempio.entity.enumerazioni.TipoTransazione.USCITA
          and t.dataTransazione between :dal and :al
        """)
    BigDecimal sommaUsciteCategoriaNelPeriodo(@Param("spazioLavoroId") Long spazioLavoroId,
                                              @Param("categoriaId") Long categoriaId,
                                              @Param("dal") LocalDate dal,
                                              @Param("al") LocalDate al);

    /** Totali per categoria nel periodo (grafico distribuzione spese) */
    @Query("""
        select t.categoria.id as categoriaId, t.categoria.nome as categoriaNome,
               t.categoria.colore as colore, sum(t.importo) as totale
        from Transazione t
        where t.spazioLavoro.id = :spazioLavoroId and t.tipo = :tipo
          and t.dataTransazione between :dal and :al
        group by t.categoria.id, t.categoria.nome, t.categoria.colore
        order by sum(t.importo) desc
        """)
    List<TotaleCategoria> sommaPerCategoriaNelPeriodo(@Param("spazioLavoroId") Long spazioLavoroId,
                                                      @Param("tipo") TipoTransazione tipo,
                                                      @Param("dal") LocalDate dal,
                                                      @Param("al") LocalDate al);

    /** Transazioni di un periodo: base per i trend settimanali/mensili e per il PDF */
    @Query("""
        select t from Transazione t join fetch t.categoria
        where t.spazioLavoro.id = :spazioLavoroId
          and t.dataTransazione between :dal and :al
        order by t.dataTransazione, t.id
        """)
    List<Transazione> trovaNelPeriodo(@Param("spazioLavoroId") Long spazioLavoroId,
                                      @Param("dal") LocalDate dal,
                                      @Param("al") LocalDate al);

    @Modifying(clearAutomatically = true)
    @Query("delete from Transazione t where t.spazioLavoro.id = :spazioLavoroId")
    void eliminaTuttePerSpazioLavoro(@Param("spazioLavoroId") Long spazioLavoroId);
}