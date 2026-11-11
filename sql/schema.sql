-- FrioOS: estrutura inicial do banco de dados MySQL.
-- O banco usa utf8mb4 para armazenar corretamente caracteres como ç e acentos.
CREATE DATABASE IF NOT EXISTS frioos
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE frioos;

-- CLIENTE
-- A chave primária (PK) identifica cada cliente de forma única.
CREATE TABLE cliente (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    telefone VARCHAR(30) NOT NULL,
    email VARCHAR(120) NULL,
    endereco VARCHAR(255) NULL
) ENGINE = InnoDB;

-- EQUIPAMENTO
-- cliente_id é uma chave estrangeira (FK): cada equipamento pertence a um cliente.
-- Assim, um cliente pode ter vários equipamentos (relacionamento 1:N).
CREATE TABLE equipamento (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id INT NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    marca VARCHAR(80) NOT NULL,
    modelo VARCHAR(100) NOT NULL,
    CONSTRAINT fk_equipamento_cliente
        FOREIGN KEY (cliente_id)
        REFERENCES cliente (id)
        ON DELETE RESTRICT
) ENGINE = InnoDB;

-- ORDEM DE SERVIÇO
-- As FKs ligam cada ordem a um cliente e a um equipamento.
-- Um cliente e um equipamento podem aparecer em várias ordens (1:N).
CREATE TABLE ordem_servico (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id INT NOT NULL,
    equipamento_id INT NOT NULL,
    defeito TEXT NOT NULL,
    status ENUM('ABERTA', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA') NOT NULL DEFAULT 'ABERTA',
    prazo_dias INT NOT NULL,
    data_abertura DATETIME NOT NULL,
    data_conclusao DATETIME NULL,
    CONSTRAINT fk_ordem_servico_cliente
        FOREIGN KEY (cliente_id)
        REFERENCES cliente (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_ordem_servico_equipamento
        FOREIGN KEY (equipamento_id)
        REFERENCES equipamento (id)
        ON DELETE RESTRICT
) ENGINE = InnoDB;

-- ITEM DE SERVIÇO
-- ordem_servico_id é uma FK. Uma ordem pode possuir vários itens (1:N).
-- Ao apagar uma ordem, seus itens são apagados junto com ON DELETE CASCADE.
CREATE TABLE item_servico (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ordem_servico_id INT NOT NULL,
    descricao VARCHAR(150) NOT NULL,
    quantidade INT NOT NULL,
    valor_unitario DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_item_servico_ordem
        FOREIGN KEY (ordem_servico_id)
        REFERENCES ordem_servico (id)
        ON DELETE CASCADE
) ENGINE = InnoDB;
