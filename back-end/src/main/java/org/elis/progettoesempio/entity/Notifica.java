// Notifica.java
package org.elis.progettoesempio.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifiche")
@Getter @Setter @NoArgsConstructor
public class Notifica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titolo;

    @Column(nullable = false, length = 500)
    private String messaggio;

    @Column(name = "data_creazione", nullable = false, updatable = false)
    private LocalDateTime dataCreazione;

    @Column(nullable = false)
    private boolean letta = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utente_id")
    private Utente utente;

    @PrePersist
    void allaCreazione() {
        dataCreazione = LocalDateTime.now();
    }
}