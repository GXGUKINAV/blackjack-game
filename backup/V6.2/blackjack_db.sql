-- Crea e seleziona il database
CREATE DATABASE IF NOT EXISTS blackjack_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE blackjack_db;

-- Tabella Giocatore
CREATE TABLE IF NOT EXISTS Giocatore (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)    NOT NULL UNIQUE,
    password   VARCHAR(255)   NOT NULL DEFAULT '',
    crediti    DECIMAL(10, 2) NOT NULL DEFAULT 1000.00
);

-- Inserisci il dealer fasullo (id fisso = 0)
-- ON DUPLICATE KEY IGNORE in caso di re-esecuzione
INSERT INTO Giocatore (id, username, password, crediti)
VALUES (0, 'Dealer', '', 0.00)
ON DUPLICATE KEY UPDATE username = 'Dealer';

-- Tabella Partita
CREATE TABLE IF NOT EXISTS Partita (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    data_ora    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabella di mezzo PartecipazionePartita
CREATE TABLE IF NOT EXISTS PartecipazionePartita (
    fk_giocatore      INT NOT NULL,
    fk_partita        INT NOT NULL,
    esito             ENUM('WIN', 'LOSE', 'DRAW') NOT NULL,
    numero_realizzato  INT NOT NULL DEFAULT 0,
    somma_scommessa   DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    player_disconnesso BOOLEAN NOT NULL DEFAULT 0,
    PRIMARY KEY (fk_giocatore, fk_partita),
    CONSTRAINT fk_gioc FOREIGN KEY (fk_giocatore) REFERENCES Giocatore(id),
    CONSTRAINT fk_part FOREIGN KEY (fk_partita) REFERENCES Partita(id)
);