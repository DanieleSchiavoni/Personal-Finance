// Transazione.java
package org.elis.progettoesempio.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.elis.progettoesempio.entity.enumerazioni.MetodoPagamento;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transazioni")
@Getter @Setter @NoArgsConstructor
public class Transazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importo;

    @Column(nullable = false, length = 255)
    private String descrizione;

    @Column(name = "data_transazione", nullable = false)
    private LocalDate dataTransazione;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoTransazione tipo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "metodo_pagamento", nullable = false, length = 20)
    private MetodoPagamento metodoPagamento;

    @Column(length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spazio_lavoro_id")
    private SpazioLavoro spazioLavoro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    /** Chi ha inserito la transazione (null se l'utente è stato eliminato) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creata_da")
    private Utente creataDa;

    /** Valorizzati solo per i pagamenti di stipendio */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dipendente_id")
    private Dipendente dipendente;

    @Column(name = "mese_stipendio")
    private Integer meseStipendio;

    @Column(name = "anno_stipendio")
    private Integer annoStipendio;
}