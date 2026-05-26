CREATE TABLE IF NOT EXISTS cliente (
    id_cliente BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    telefone VARCHAR(30) NOT NULL,
    cpf VARCHAR(20),
    endereco VARCHAR(255),
    email VARCHAR(120),
    observacoes TEXT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS produto (
    id_produto BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    categoria VARCHAR(80) NOT NULL,
    descricao TEXT,
    quantidade_estoque INTEGER NOT NULL DEFAULT 0,
    estoque_minimo INTEGER NOT NULL DEFAULT 0,
    valor_compra NUMERIC(12, 2) NOT NULL DEFAULT 0,
    valor_venda NUMERIC(12, 2) NOT NULL DEFAULT 0,
    foto BYTEA,
    foto_nome VARCHAR(180),
    foto_tipo VARCHAR(80),
    foto_tamanho INTEGER,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_produto_foto_tamanho CHECK (foto_tamanho IS NULL OR foto_tamanho <= 512000)
);

CREATE TABLE IF NOT EXISTS equipamento (
    id_equipamento BIGSERIAL PRIMARY KEY,
    marca VARCHAR(80) NOT NULL,
    tipo_equipamento VARCHAR(80) NOT NULL,
    modelo VARCHAR(120) NOT NULL,
    descricao TEXT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS usuario_sistema (
    id_usuario BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    login VARCHAR(80) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    perfil VARCHAR(40) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO usuario_sistema (nome, login, senha, perfil, ativo)
VALUES ('Administrador', 'admin', '1234', 'ADMINISTRADOR', TRUE)
ON CONFLICT (login) DO NOTHING;

CREATE TABLE IF NOT EXISTS ordem_servico (
    id_ordem_servico BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL,
    id_equipamento BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    numero_serie VARCHAR(120),
    cor VARCHAR(60),
    data_entrada DATE NOT NULL DEFAULT CURRENT_DATE,
    problema_relatado TEXT NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'RECEBIDO',
    diagnostico TEXT,
    servico_realizado TEXT,
    valor_servico NUMERIC(12, 2),
    data_saida DATE,
    observacoes TEXT,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ordem_servico_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT fk_ordem_servico_equipamento FOREIGN KEY (id_equipamento) REFERENCES equipamento (id_equipamento),
    CONSTRAINT fk_ordem_servico_usuario FOREIGN KEY (id_usuario) REFERENCES usuario_sistema (id_usuario)
);
