-- Banco de dados do Controle Ágil (SQLite)
--
-- Valores em dinheiro ficam em TEXT (ex.: '12.50') para não perder centavos:
-- o Java lê com new BigDecimal(texto). Datas ficam em TEXT no formato ISO 8601
-- (ex.: '2026-10-23T14:30'), que o LocalDateTime lê e escreve direto.
-- Todos os comandos usam IF NOT EXISTS: rodar este script de novo não apaga nada.

CREATE TABLE IF NOT EXISTS cliente (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    nome          TEXT    NOT NULL,
    telefone      TEXT,
    endereco      TEXT,
    limite_fiado  TEXT                -- NULL quando o cliente não tem conta fiado
);

CREATE TABLE IF NOT EXISTS produto (
    id     INTEGER PRIMARY KEY AUTOINCREMENT,
    nome   TEXT NOT NULL,
    preco  TEXT NOT NULL CHECK (CAST(preco AS REAL) > 0)
);

CREATE TABLE IF NOT EXISTS pedido (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    cliente_id         INTEGER NOT NULL REFERENCES cliente (id),
    data_criacao       TEXT    NOT NULL,
    data_hora_entrega  TEXT    NOT NULL,
    status             TEXT    NOT NULL DEFAULT 'PENDENTE'
        CHECK (status IN ('PENDENTE', 'EM_PRODUCAO', 'SAIU_PARA_ENTREGA', 'ENTREGUE', 'CANCELADO'))
);

CREATE TABLE IF NOT EXISTS item_pedido (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    pedido_id       INTEGER NOT NULL REFERENCES pedido (id) ON DELETE CASCADE,
    produto_id      INTEGER NOT NULL REFERENCES produto (id),
    quantidade      INTEGER NOT NULL CHECK (quantidade > 0),
    preco_unitario  TEXT    NOT NULL   -- preço do dia da venda
);

CREATE TABLE IF NOT EXISTS pagamento (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    pedido_id         INTEGER NOT NULL REFERENCES pedido (id),
    forma             TEXT    NOT NULL CHECK (forma IN ('DINHEIRO', 'PIX', 'FIADO')),
    valor             TEXT    NOT NULL,
    data              TEXT    NOT NULL,
    valor_recebido    TEXT,              -- só no dinheiro
    codigo_transacao  TEXT               -- só no Pix
);

CREATE TABLE IF NOT EXISTS movimentacao_fiado (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    cliente_id  INTEGER NOT NULL REFERENCES cliente (id),
    tipo        TEXT    NOT NULL CHECK (tipo IN ('COMPRA', 'PAGAMENTO')),
    valor       TEXT    NOT NULL,
    data        TEXT    NOT NULL,
    pedido_id   INTEGER REFERENCES pedido (id)   -- só nas compras
);

CREATE INDEX IF NOT EXISTS idx_pedido_entrega ON pedido (data_hora_entrega);
CREATE INDEX IF NOT EXISTS idx_movimentacao_cliente ON movimentacao_fiado (cliente_id);
