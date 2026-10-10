// SpazioLavoro.java
package org.elis.progettoesempio.entity;

import java.time.LocalDateTime;
import org.elis.progettoesempio.entity.enumerazioni.TipoSpazio;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "spazi_lavoro")
@Getter @Setter @NoArgsConstructor
public class SpazioLavoro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoSpazio tipo;

    @Column(nullable = false, length = 3)
    private String valuta = "EUR";

    @Column(name = "data_creazione", nullable = false, updatable = false)
    private LocalDateTime dataCreazione;

    @PrePersist
    void allaCreazione() {
        dataCreazione = LocalDateTime.now();
    }
}