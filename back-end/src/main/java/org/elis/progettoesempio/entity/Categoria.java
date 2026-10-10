// Categoria.java
package org.elis.progettoesempio.entity;

import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.elis.progettoesempio.entity.enumerazioni.TipoTransazione;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categorie")
@Getter @Setter @NoArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoTransazione tipo;

    @Column(length = 7)
    private String colore;

    @Column(length = 255)
    private String descrizione;

    /** null = categoria globale, gestita dall'admin di piattaforma */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spazio_lavoro_id")
    private SpazioLavoro spazioLavoro;

    /** Solo per le globali: null = visibile a tutti i tipi di spazio */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "tipo_spazio", length = 20)
    private TipoSpazio tipoSpazio;
}