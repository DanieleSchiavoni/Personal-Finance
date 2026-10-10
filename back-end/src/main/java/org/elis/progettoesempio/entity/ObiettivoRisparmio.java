// ObiettivoRisparmio.java
package org.elis.progettoesempio.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.elis.progettoesempio.entity.enumerazioni.StatoObiettivo;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "obiettivi_risparmio")
@Getter @Setter @NoArgsConstructor
public class ObiettivoRisparmio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 255)
    private String descrizione;

    @Column(name = "importo_obiettivo", nullable = false, precision = 12, scale = 2)
    private BigDecimal importoObiettivo;

    @Column(name = "importo_attuale", nullable = false, precision = 12, scale = 2)
    private BigDecimal importoAttuale = BigDecimal.ZERO;

    private LocalDate scadenza;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private StatoObiettivo stato = StatoObiettivo.IN_CORSO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spazio_lavoro_id")
    private SpazioLavoro spazioLavoro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creato_da")
    private Utente creatoDa;
}