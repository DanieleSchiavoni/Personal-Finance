// Utente.java
package org.elis.progettoesempio.entity;

import java.time.LocalDateTime;
import org.elis.progettoesempio.entity.enumerazioni.RuoloUtente;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "utenti")
@Getter @Setter @NoArgsConstructor
public class Utente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** Hash BCrypt, mai la password in chiaro */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 100)
    private String cognome;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private RuoloUtente ruolo = RuoloUtente.UTENTE;

    @Column(nullable = false)
    private boolean abilitato = true;

    @Column(name = "data_creazione", nullable = false, updatable = false)
    private LocalDateTime dataCreazione;

    @PrePersist
    void allaCreazione() {
        dataCreazione = LocalDateTime.now();
    }
}