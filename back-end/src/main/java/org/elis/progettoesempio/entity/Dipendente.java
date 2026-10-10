// Dipendente.java
package org.elis.progettoesempio.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dipendenti")
@Getter @Setter @NoArgsConstructor
public class Dipendente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spazio_lavoro_id")
    private SpazioLavoro spazioLavoro;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 100)
    private String cognome;

    @Column(length = 100)
    private String mansione;

    @Column(name = "stipendio_mensile", nullable = false, precision = 12, scale = 2)
    private BigDecimal stipendioMensile;

    @Column(name = "data_assunzione")
    private LocalDate dataAssunzione;

    @Column(nullable = false)
    private boolean attivo = true;

    /** Collegamento opzionale a un account, se il dipendente usa l'app */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utente_id")
    private Utente utente;
}