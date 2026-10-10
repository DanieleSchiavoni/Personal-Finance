CREATE TABLE utenti (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL,
    email          VARCHAR(150) NOT NULL,
    password       VARCHAR(100) NOT NULL,
    nome           VARCHAR(100) NOT NULL,
    cognome        VARCHAR(100) NOT NULL,
    ruolo          VARCHAR(20)  NOT NULL DEFAULT 'UTENTE',
    abilitato      BOOLEAN      NOT NULL DEFAULT TRUE,
    data_creazione DATETIME(6)  NOT NULL,
    CONSTRAINT uq_utenti_username UNIQUE (username),
    CONSTRAINT uq_utenti_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE spazi_lavoro (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome           VARCHAR(100) NOT NULL,
    tipo           VARCHAR(20)  NOT NULL,
    valuta         VARCHAR(3)   NOT NULL DEFAULT 'EUR',
    data_creazione DATETIME(6)  NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE membri_spazio (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    spazio_lavoro_id BIGINT      NOT NULL,
    utente_id        BIGINT      NOT NULL,
    ruolo            VARCHAR(20) NOT NULL,
    data_ingresso    DATETIME(6) NOT NULL,
    CONSTRAINT uq_membro UNIQUE (spazio_lavoro_id, utente_id),
    CONSTRAINT fk_membro_spazio FOREIGN KEY (spazio_lavoro_id) REFERENCES spazi_lavoro (id) ON DELETE CASCADE,
    CONSTRAINT fk_membro_utente FOREIGN KEY (utente_id) REFERENCES utenti (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE categorie (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome             VARCHAR(100) NOT NULL,
    tipo             VARCHAR(20)  NOT NULL,
    colore           VARCHAR(7),
    descrizione      VARCHAR(255),
    spazio_lavoro_id BIGINT      NULL,
    tipo_spazio      VARCHAR(20) NULL,
    CONSTRAINT fk_categoria_spazio FOREIGN KEY (spazio_lavoro_id) REFERENCES spazi_lavoro (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dipendenti (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    spazio_lavoro_id  BIGINT        NOT NULL,
    nome              VARCHAR(100)  NOT NULL,
    cognome           VARCHAR(100)  NOT NULL,
    mansione          VARCHAR(100),
    stipendio_mensile DECIMAL(12,2) NOT NULL,
    data_assunzione   DATE,
    attivo            BOOLEAN       NOT NULL DEFAULT TRUE,
    utente_id         BIGINT NULL,
    CONSTRAINT chk_dipendente_stipendio CHECK (stipendio_mensile >= 0),
    CONSTRAINT fk_dipendente_spazio FOREIGN KEY (spazio_lavoro_id) REFERENCES spazi_lavoro (id) ON DELETE CASCADE,
    CONSTRAINT fk_dipendente_utente FOREIGN KEY (utente_id) REFERENCES utenti (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE transazioni (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    importo           DECIMAL(12,2) NOT NULL,
    descrizione       VARCHAR(255)  NOT NULL,
    data_transazione  DATE          NOT NULL,
    tipo              VARCHAR(20)   NOT NULL,
    metodo_pagamento  VARCHAR(20)   NOT NULL,
    note              VARCHAR(500),
    spazio_lavoro_id  BIGINT NOT NULL,
    categoria_id      BIGINT NOT NULL,
    creata_da         BIGINT NULL,
    dipendente_id     BIGINT NULL,
    mese_stipendio    INT NULL,
    anno_stipendio    INT NULL,
    CONSTRAINT chk_transazione_importo CHECK (importo > 0),
    CONSTRAINT uq_pagamento_stipendio UNIQUE (dipendente_id, anno_stipendio, mese_stipendio),
    CONSTRAINT fk_transazione_spazio FOREIGN KEY (spazio_lavoro_id) REFERENCES spazi_lavoro (id) ON DELETE CASCADE,
    CONSTRAINT fk_transazione_categoria FOREIGN KEY (categoria_id) REFERENCES categorie (id) ON DELETE RESTRICT,
    CONSTRAINT fk_transazione_creata_da FOREIGN KEY (creata_da) REFERENCES utenti (id) ON DELETE SET NULL,
    CONSTRAINT fk_transazione_dipendente FOREIGN KEY (dipendente_id) REFERENCES dipendenti (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_transazioni_spazio_data ON transazioni (spazio_lavoro_id, data_transazione);
CREATE INDEX idx_transazioni_categoria ON transazioni (categoria_id);

CREATE TABLE budget (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    importo          DECIMAL(12,2) NOT NULL,
    mese             INT NOT NULL,
    anno             INT NOT NULL,
    spazio_lavoro_id BIGINT NOT NULL,
    categoria_id     BIGINT NOT NULL,
    CONSTRAINT chk_budget_importo CHECK (importo > 0),
    CONSTRAINT chk_budget_mese CHECK (mese BETWEEN 1 AND 12),
    CONSTRAINT uq_budget UNIQUE (spazio_lavoro_id, categoria_id, mese, anno),
    CONSTRAINT fk_budget_spazio FOREIGN KEY (spazio_lavoro_id) REFERENCES spazi_lavoro (id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_categoria FOREIGN KEY (categoria_id) REFERENCES categorie (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE obiettivi_risparmio (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome             VARCHAR(100)  NOT NULL,
    descrizione      VARCHAR(255),
    importo_obiettivo DECIMAL(12,2) NOT NULL,
    importo_attuale  DECIMAL(12,2) NOT NULL DEFAULT 0,
    scadenza         DATE,
    stato            VARCHAR(20)   NOT NULL DEFAULT 'IN_CORSO',
    spazio_lavoro_id BIGINT NOT NULL,
    creato_da        BIGINT NULL,
    CONSTRAINT chk_obiettivo_importo CHECK (importo_obiettivo > 0),
    CONSTRAINT chk_obiettivo_attuale CHECK (importo_attuale >= 0),
    CONSTRAINT fk_obiettivo_spazio FOREIGN KEY (spazio_lavoro_id) REFERENCES spazi_lavoro (id) ON DELETE CASCADE,
    CONSTRAINT fk_obiettivo_creato_da FOREIGN KEY (creato_da) REFERENCES utenti (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notifiche (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    titolo         VARCHAR(150) NOT NULL,
    messaggio      VARCHAR(500) NOT NULL,
    data_creazione DATETIME(6)  NOT NULL,
    letta          BOOLEAN      NOT NULL DEFAULT FALSE,
    utente_id      BIGINT       NOT NULL,
    CONSTRAINT fk_notifica_utente FOREIGN KEY (utente_id) REFERENCES utenti (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refresh_token (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    token          VARCHAR(255) NOT NULL,
    data_scadenza  DATETIME(6)  NOT NULL,
    utente_id      BIGINT       NOT NULL,
    CONSTRAINT uq_refresh_token UNIQUE (token),
    CONSTRAINT fk_refresh_utente FOREIGN KEY (utente_id) REFERENCES utenti (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Categorie globali (spazio_lavoro_id NULL). tipo_spazio NULL = visibile a tutti gli spazi
INSERT INTO categorie (nome, tipo, colore, descrizione, spazio_lavoro_id, tipo_spazio) VALUES
('Stipendio',       'ENTRATA', '#198754', NULL, NULL, 'PERSONALE'),
('Freelance',       'ENTRATA', '#20c997', NULL, NULL, 'PERSONALE'),
('Investimenti',    'ENTRATA', '#0dcaf0', NULL, NULL, NULL),
('Bonus',           'ENTRATA', '#6f42c1', NULL, NULL, 'PERSONALE'),
('Vendite',         'ENTRATA', '#198754', NULL, NULL, 'AZIENDALE'),
('Servizi',         'ENTRATA', '#20c997', NULL, NULL, 'AZIENDALE'),
('Altro',           'ENTRATA', '#6c757d', NULL, NULL, NULL),
('Casa',            'USCITA',  '#fd7e14', NULL, NULL, 'PERSONALE'),
('Alimentari',      'USCITA',  '#dc3545', NULL, NULL, 'PERSONALE'),
('Trasporti',       'USCITA',  '#0d6efd', NULL, NULL, NULL),
('Ristoranti',      'USCITA',  '#d63384', NULL, NULL, 'PERSONALE'),
('Shopping',        'USCITA',  '#6f42c1', NULL, NULL, 'PERSONALE'),
('Intrattenimento', 'USCITA',  '#ffc107', NULL, NULL, 'PERSONALE'),
('Salute',          'USCITA',  '#20c997', NULL, NULL, 'PERSONALE'),
('Istruzione',      'USCITA',  '#0dcaf0', NULL, NULL, NULL),
('Abbonamenti',     'USCITA',  '#6610f2', NULL, NULL, NULL),
('Bollette',        'USCITA',  '#fd7e14', NULL, NULL, NULL),
('Stipendi',        'USCITA',  '#dc3545', NULL, NULL, 'AZIENDALE'),
('Fornitori',       'USCITA',  '#0d6efd', NULL, NULL, 'AZIENDALE'),
('Affitto',         'USCITA',  '#fd7e14', NULL, NULL, 'AZIENDALE'),
('Tasse',           'USCITA',  '#6c757d', NULL, NULL, 'AZIENDALE'),
('Marketing',       'USCITA',  '#d63384', NULL, NULL, 'AZIENDALE'),
('Altro',           'USCITA',  '#6c757d', NULL, NULL, NULL);