// Budget.java
package org.elis.progettoesempio.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "budget")
@Getter @Setter @NoArgsConstructor
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importo;

    @Column(nullable = false)
    private int mese;

    @Column(nullable = false)
    private int anno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spazio_lavoro_id")
    private SpazioLavoro spazioLavoro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
}