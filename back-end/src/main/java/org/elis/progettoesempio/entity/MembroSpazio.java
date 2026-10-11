package org.elis.progettoesempio.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "membri_spazio",
       uniqueConstraints = @UniqueConstraint(name = "uq_membro",
               columnNames = {"spazio_lavoro_id", "utente_id"}))
@Getter @Setter @NoArgsConstructor
public class MembroSpazio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spazio_lavoro_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SpazioLavoro spazioLavoro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utente_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Utente utente;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private RuoloSpazio ruolo;

    @Column(name = "data_ingresso", nullable = false, updatable = false)
    private LocalDateTime dataIngresso;

    @PrePersist
    void allaCreazione() {
        dataIngresso = LocalDateTime.now();
    }
}