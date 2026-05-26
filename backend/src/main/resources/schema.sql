-- =====================================================
-- BANCO DE DADOS: acaogames
-- Sistema para Loja de Videogames
-- PostgreSQL
-- =====================================================
-- Execute este script dentro do banco acaogames.
-- Nao execute CREATE DATABASE aqui.
-- Atencao: este script recria as tabelas e reinsere dados iniciais.

DROP TABLE IF EXISTS item_venda CASCADE;
DROP TABLE IF EXISTS item_servico CASCADE;
DROP TABLE IF EXISTS movimentacao_estoque CASCADE;
DROP TABLE IF EXISTS movimento_caixa CASCADE;
DROP TABLE IF EXISTS garantia CASCADE;
DROP TABLE IF EXISTS orcamento CASCADE;
DROP TABLE IF EXISTS venda CASCADE;
DROP TABLE IF EXISTS ordem_servico CASCADE;
DROP TABLE IF EXISTS produto CASCADE;
DROP TABLE IF EXISTS equipamento CASCADE;
DROP TABLE IF EXISTS usuario_sistema CASCADE;
DROP TABLE IF EXISTS cliente CASCADE;

CREATE TABLE cliente (
    id_cliente BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    cpf VARCHAR(14),
    endereco VARCHAR(200),
    email VARCHAR(120),
    observacoes TEXT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuario_sistema (
    id_usuario BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    login VARCHAR(60) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    perfil VARCHAR(30) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_usuario_perfil CHECK (perfil IN ('ADMINISTRADOR', 'ATENDENTE', 'TECNICO', 'FINANCEIRO'))
);

CREATE TABLE equipamento (
    id_equipamento BIGSERIAL PRIMARY KEY,
    marca VARCHAR(50) NOT NULL,
    tipo_equipamento VARCHAR(30) NOT NULL,
    modelo VARCHAR(100) NOT NULL,
    descricao TEXT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_equipamento_marca CHECK (marca IN ('Sony', 'Microsoft', 'Nintendo')),
    CONSTRAINT chk_equipamento_tipo CHECK (tipo_equipamento IN ('Console', 'Controle')),
    CONSTRAINT uk_equipamento_modelo UNIQUE (marca, tipo_equipamento, modelo)
);

CREATE TABLE produto (
    id_produto BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    categoria VARCHAR(60) NOT NULL,
    descricao TEXT,
    quantidade_estoque INTEGER NOT NULL DEFAULT 0,
    estoque_minimo INTEGER NOT NULL DEFAULT 0,
    valor_compra NUMERIC(10,2) NOT NULL DEFAULT 0,
    valor_venda NUMERIC(10,2) NOT NULL DEFAULT 0,
    foto BYTEA,
    foto_nome VARCHAR(180),
    foto_tipo VARCHAR(80),
    foto_tamanho INTEGER,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_produto_quantidade CHECK (quantidade_estoque >= 0),
    CONSTRAINT chk_produto_estoque_minimo CHECK (estoque_minimo >= 0),
    CONSTRAINT chk_produto_valor_compra CHECK (valor_compra >= 0),
    CONSTRAINT chk_produto_valor_venda CHECK (valor_venda >= 0),
    CONSTRAINT chk_produto_foto_tamanho CHECK (foto_tamanho IS NULL OR foto_tamanho <= 512000)
);

CREATE TABLE ordem_servico (
    id_ordem_servico BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL,
    id_equipamento BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    numero_serie VARCHAR(100),
    cor VARCHAR(50),
    data_entrada DATE NOT NULL DEFAULT CURRENT_DATE,
    problema_relatado TEXT NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'RECEBIDO',
    diagnostico TEXT,
    servico_realizado TEXT,
    valor_servico NUMERIC(10,2),
    data_saida DATE,
    observacoes TEXT,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_os_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_os_equipamento FOREIGN KEY (id_equipamento) REFERENCES equipamento(id_equipamento) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_os_usuario FOREIGN KEY (id_usuario) REFERENCES usuario_sistema(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_os_status CHECK (status IN (
        'RECEBIDO',
        'EM_ANALISE',
        'AGUARDANDO_APROVACAO',
        'APROVADO',
        'EM_MANUTENCAO',
        'PRONTO',
        'ENTREGUE',
        'CANCELADO'
    )),
    CONSTRAINT chk_os_valor_servico CHECK (valor_servico IS NULL OR valor_servico >= 0),
    CONSTRAINT chk_os_datas CHECK (data_saida IS NULL OR data_saida >= data_entrada)
);

CREATE TABLE orcamento (
    id_orcamento BIGSERIAL PRIMARY KEY,
    id_ordem_servico BIGINT NOT NULL UNIQUE,
    descricao TEXT NOT NULL,
    valor_estimado NUMERIC(10,2) NOT NULL,
    data_orcamento DATE NOT NULL DEFAULT CURRENT_DATE,
    status_orcamento VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    observacoes TEXT,
    CONSTRAINT fk_orcamento_os FOREIGN KEY (id_ordem_servico) REFERENCES ordem_servico(id_ordem_servico) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_orcamento_valor CHECK (valor_estimado >= 0),
    CONSTRAINT chk_orcamento_status CHECK (status_orcamento IN ('PENDENTE', 'APROVADO', 'RECUSADO'))
);

CREATE TABLE garantia (
    id_garantia BIGSERIAL PRIMARY KEY,
    id_ordem_servico BIGINT NOT NULL UNIQUE,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    tempo_garantia_dias INTEGER NOT NULL,
    descricao TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ATIVA',
    CONSTRAINT fk_garantia_os FOREIGN KEY (id_ordem_servico) REFERENCES ordem_servico(id_ordem_servico) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_garantia_tempo CHECK (tempo_garantia_dias > 0),
    CONSTRAINT chk_garantia_datas CHECK (data_fim >= data_inicio),
    CONSTRAINT chk_garantia_status CHECK (status IN ('ATIVA', 'EXPIRADA', 'CANCELADA'))
);

CREATE TABLE item_servico (
    id_item_servico BIGSERIAL PRIMARY KEY,
    id_ordem_servico BIGINT NOT NULL,
    id_produto BIGINT NOT NULL,
    quantidade INTEGER NOT NULL,
    valor_unitario NUMERIC(10,2) NOT NULL,
    subtotal NUMERIC(10,2) NOT NULL,
    CONSTRAINT fk_item_servico_os FOREIGN KEY (id_ordem_servico) REFERENCES ordem_servico(id_ordem_servico) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_item_servico_produto FOREIGN KEY (id_produto) REFERENCES produto(id_produto) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_item_servico_quantidade CHECK (quantidade > 0),
    CONSTRAINT chk_item_servico_valor CHECK (valor_unitario >= 0),
    CONSTRAINT chk_item_servico_subtotal CHECK (subtotal >= 0)
);

CREATE TABLE venda (
    id_venda BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT,
    data_venda DATE NOT NULL DEFAULT CURRENT_DATE,
    valor_total NUMERIC(10,2) NOT NULL,
    forma_pagamento VARCHAR(30) NOT NULL,
    observacoes TEXT,
    CONSTRAINT fk_venda_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_venda_valor CHECK (valor_total >= 0),
    CONSTRAINT chk_venda_forma_pagamento CHECK (forma_pagamento IN ('DINHEIRO', 'PIX', 'CARTAO_DEBITO', 'CARTAO_CREDITO'))
);

CREATE TABLE item_venda (
    id_item_venda BIGSERIAL PRIMARY KEY,
    id_venda BIGINT NOT NULL,
    id_produto BIGINT NOT NULL,
    quantidade INTEGER NOT NULL,
    valor_unitario NUMERIC(10,2) NOT NULL,
    subtotal NUMERIC(10,2) NOT NULL,
    CONSTRAINT fk_item_venda_venda FOREIGN KEY (id_venda) REFERENCES venda(id_venda) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_item_venda_produto FOREIGN KEY (id_produto) REFERENCES produto(id_produto) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_item_venda_quantidade CHECK (quantidade > 0),
    CONSTRAINT chk_item_venda_valor CHECK (valor_unitario >= 0),
    CONSTRAINT chk_item_venda_subtotal CHECK (subtotal >= 0)
);

CREATE TABLE movimentacao_estoque (
    id_movimentacao BIGSERIAL PRIMARY KEY,
    id_produto BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    quantidade INTEGER NOT NULL,
    data_movimentacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo VARCHAR(100) NOT NULL,
    observacoes TEXT,
    CONSTRAINT fk_movimentacao_produto FOREIGN KEY (id_produto) REFERENCES produto(id_produto) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_movimentacao_usuario FOREIGN KEY (id_usuario) REFERENCES usuario_sistema(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_movimentacao_tipo CHECK (tipo IN ('ENTRADA', 'SAIDA')),
    CONSTRAINT chk_movimentacao_quantidade CHECK (quantidade > 0)
);

CREATE TABLE movimento_caixa (
    id_movimento_caixa BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    id_venda BIGINT UNIQUE,
    id_ordem_servico BIGINT UNIQUE,
    tipo VARCHAR(20) NOT NULL,
    descricao TEXT NOT NULL,
    valor NUMERIC(10,2) NOT NULL,
    data_movimento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    forma_pagamento VARCHAR(30),
    categoria VARCHAR(60) NOT NULL,
    CONSTRAINT fk_caixa_usuario FOREIGN KEY (id_usuario) REFERENCES usuario_sistema(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_caixa_venda FOREIGN KEY (id_venda) REFERENCES venda(id_venda) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_caixa_os FOREIGN KEY (id_ordem_servico) REFERENCES ordem_servico(id_ordem_servico) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_caixa_tipo CHECK (tipo IN ('ENTRADA', 'SAIDA')),
    CONSTRAINT chk_caixa_valor CHECK (valor >= 0),
    CONSTRAINT chk_caixa_forma_pagamento CHECK (
        forma_pagamento IS NULL OR forma_pagamento IN ('DINHEIRO', 'PIX', 'CARTAO_DEBITO', 'CARTAO_CREDITO')
    )
);

CREATE INDEX idx_cliente_nome ON cliente(nome);
CREATE INDEX idx_cliente_telefone ON cliente(telefone);
CREATE INDEX idx_cliente_cpf ON cliente(cpf);
CREATE INDEX idx_equipamento_marca ON equipamento(marca);
CREATE INDEX idx_equipamento_tipo ON equipamento(tipo_equipamento);
CREATE INDEX idx_equipamento_modelo ON equipamento(modelo);
CREATE INDEX idx_os_cliente ON ordem_servico(id_cliente);
CREATE INDEX idx_os_equipamento ON ordem_servico(id_equipamento);
CREATE INDEX idx_os_usuario ON ordem_servico(id_usuario);
CREATE INDEX idx_os_status ON ordem_servico(status);
CREATE INDEX idx_os_data_entrada ON ordem_servico(data_entrada);
CREATE INDEX idx_os_numero_serie ON ordem_servico(numero_serie);
CREATE INDEX idx_produto_nome ON produto(nome);
CREATE INDEX idx_produto_categoria ON produto(categoria);
CREATE INDEX idx_venda_data ON venda(data_venda);
CREATE INDEX idx_caixa_data ON movimento_caixa(data_movimento);
CREATE INDEX idx_caixa_tipo ON movimento_caixa(tipo);
CREATE INDEX idx_caixa_categoria ON movimento_caixa(categoria);
CREATE INDEX idx_estoque_data ON movimentacao_estoque(data_movimentacao);
CREATE INDEX idx_estoque_produto ON movimentacao_estoque(id_produto);

INSERT INTO usuario_sistema (nome, login, senha, perfil, ativo) VALUES
('Administrador Geral', 'admin', '123456', 'ADMINISTRADOR', TRUE),
('Atendente Padrao', 'atendente', '123456', 'ATENDENTE', TRUE),
('Tecnico Padrao', 'tecnico', '123456', 'TECNICO', TRUE),
('Financeiro Padrao', 'financeiro', '123456', 'FINANCEIRO', TRUE);

INSERT INTO cliente (nome, telefone, cpf, endereco, email, observacoes) VALUES
('Joao Silva', '(28) 99999-1111', '123.456.789-00', 'Rua das Flores, 100', 'joao@email.com', 'Cliente frequente'),
('Maria Oliveira', '(28) 99999-2222', '222.333.444-55', 'Av. Central, 250', 'maria@email.com', 'Prefere contato por WhatsApp'),
('Pedro Santos', '(28) 99999-3333', '333.444.555-66', 'Rua Projetada, 45', 'pedro@email.com', NULL),
('Lucas Almeida', '(28) 99999-4444', '444.555.666-77', 'Rua Nova, 80', 'lucas@email.com', NULL),
('Fernanda Lima', '(28) 99999-5555', '555.666.777-88', 'Av. Brasil, 900', 'fernanda@email.com', NULL);

INSERT INTO equipamento (marca, tipo_equipamento, modelo, descricao) VALUES
('Sony', 'Console', 'PlayStation 3 Super Slim', 'Console Sony PlayStation 3 Super Slim'),
('Sony', 'Console', 'PlayStation 4 Fat', 'Console Sony PlayStation 4 Fat'),
('Sony', 'Console', 'PlayStation 4 Slim', 'Console Sony PlayStation 4 Slim'),
('Sony', 'Console', 'PlayStation 4 Pro', 'Console Sony PlayStation 4 Pro'),
('Sony', 'Console', 'PlayStation 5', 'Console Sony PlayStation 5'),
('Sony', 'Controle', 'DualShock 3', 'Controle oficial do PlayStation 3'),
('Sony', 'Controle', 'DualShock 4', 'Controle oficial do PlayStation 4'),
('Sony', 'Controle', 'DualSense', 'Controle oficial do PlayStation 5'),
('Microsoft', 'Console', 'Xbox 360', 'Console Microsoft Xbox 360'),
('Microsoft', 'Console', 'Xbox One', 'Console Microsoft Xbox One'),
('Microsoft', 'Console', 'Xbox One S', 'Console Microsoft Xbox One S'),
('Microsoft', 'Console', 'Xbox One X', 'Console Microsoft Xbox One X'),
('Microsoft', 'Console', 'Xbox Series S', 'Console Microsoft Xbox Series S'),
('Microsoft', 'Console', 'Xbox Series X', 'Console Microsoft Xbox Series X'),
('Microsoft', 'Controle', 'Controle Xbox 360', 'Controle oficial do Xbox 360'),
('Microsoft', 'Controle', 'Controle Xbox One', 'Controle oficial do Xbox One'),
('Microsoft', 'Controle', 'Controle Xbox Series', 'Controle oficial do Xbox Series'),
('Nintendo', 'Console', 'Nintendo Wii', 'Console Nintendo Wii'),
('Nintendo', 'Console', 'Nintendo Wii U', 'Console Nintendo Wii U'),
('Nintendo', 'Console', 'Nintendo Switch', 'Console Nintendo Switch'),
('Nintendo', 'Console', 'Nintendo Switch Lite', 'Console Nintendo Switch Lite'),
('Nintendo', 'Console', 'Nintendo Switch OLED', 'Console Nintendo Switch OLED'),
('Nintendo', 'Controle', 'Joy-Con', 'Controle Joy-Con do Nintendo Switch'),
('Nintendo', 'Controle', 'Pro Controller', 'Controle Pro Controller do Nintendo Switch');

INSERT INTO produto (nome, categoria, descricao, quantidade_estoque, estoque_minimo, valor_compra, valor_venda) VALUES
('Cabo HDMI 2.0', 'Acessorio', 'Cabo HDMI para consoles', 20, 5, 15.00, 35.00),
('Fonte PS4 Slim', 'Peca', 'Fonte compativel com PlayStation 4 Slim', 5, 2, 120.00, 220.00),
('Pasta termica', 'Insumo', 'Pasta termica para manutencao de consoles', 15, 3, 20.00, 45.00),
('Analogico DualShock 4', 'Peca', 'Analogico para controle PS4', 12, 4, 12.00, 35.00),
('Botao RB Xbox Series', 'Peca', 'Botao RB para controle Xbox Series', 8, 2, 8.00, 25.00),
('Conector HDMI PS4', 'Peca', 'Conector HDMI para placa de PS4', 6, 2, 18.00, 60.00),
('Controle PS4 Usado', 'Produto', 'Controle PS4 usado revisado', 4, 1, 90.00, 180.00),
('Controle Xbox Usado', 'Produto', 'Controle Xbox usado revisado', 3, 1, 100.00, 200.00),
('Cabo USB-C', 'Acessorio', 'Cabo USB-C para controle e Nintendo Switch', 18, 5, 10.00, 30.00),
('Carregador Nintendo Switch', 'Acessorio', 'Fonte/carregador para Nintendo Switch', 5, 2, 85.00, 160.00);

INSERT INTO ordem_servico (
    id_cliente, id_equipamento, id_usuario, numero_serie, cor, data_entrada,
    problema_relatado, status, diagnostico, servico_realizado, valor_servico,
    data_saida, observacoes
) VALUES
(1, 3, 2, 'PS4SLIM-ABC123', 'Preto', '2026-05-01', 'Console liga, mas nao aparece imagem na TV.', 'ENTREGUE', 'Conector HDMI danificado.', 'Troca do conector HDMI e limpeza interna.', 250.00, '2026-05-04', 'Cliente retirou e testou na loja.'),
(2, 5, 2, 'PS5-XYZ987', 'Branco', '2026-05-02', 'PS5 esquenta muito e desliga.', 'PRONTO', 'Acumulo de poeira e pasta termica ressecada.', 'Limpeza interna e troca de pasta termica.', 180.00, NULL, 'Aguardando retirada.'),
(3, 11, 2, 'XONE-S-456DEF', 'Branco', '2026-05-03', 'Xbox desliga sozinho apos alguns minutos.', 'EM_MANUTENCAO', 'Fonte com instabilidade.', NULL, NULL, NULL, 'Tecnico aguardando peca.'),
(4, 20, 2, 'NSW-789GHI', 'Cinza', '2026-05-05', 'Nintendo Switch nao carrega.', 'AGUARDANDO_APROVACAO', 'Possivel defeito no conector de carga.', NULL, NULL, NULL, 'Orcamento enviado ao cliente.'),
(5, 7, 2, 'DS4-111AAA', 'Preto', '2026-05-06', 'Controle com analogico puxando para esquerda.', 'ENTREGUE', 'Drift no analogico esquerdo.', 'Troca do analogico esquerdo.', 90.00, '2026-05-07', 'Garantia de 30 dias.');

INSERT INTO orcamento (id_ordem_servico, descricao, valor_estimado, data_orcamento, status_orcamento, observacoes) VALUES
(1, 'Troca do conector HDMI e limpeza interna.', 250.00, '2026-05-02', 'APROVADO', 'Cliente aprovou via WhatsApp.'),
(2, 'Limpeza completa e troca de pasta termica.', 180.00, '2026-05-03', 'APROVADO', 'Cliente autorizou o servico.'),
(3, 'Troca da fonte interna.', 320.00, '2026-05-04', 'APROVADO', 'Peca em separacao.'),
(4, 'Troca ou reparo do conector de carga.', 210.00, '2026-05-05', 'PENDENTE', 'Aguardando retorno do cliente.'),
(5, 'Troca do analogico esquerdo.', 90.00, '2026-05-06', 'APROVADO', NULL);

INSERT INTO garantia (id_ordem_servico, data_inicio, data_fim, tempo_garantia_dias, descricao, status) VALUES
(1, '2026-05-04', '2026-08-02', 90, 'Garantia do servico de troca do HDMI.', 'ATIVA'),
(5, '2026-05-07', '2026-06-06', 30, 'Garantia do analogico substituido.', 'ATIVA');

INSERT INTO item_servico (id_ordem_servico, id_produto, quantidade, valor_unitario, subtotal) VALUES
(1, 6, 1, 60.00, 60.00),
(1, 3, 1, 45.00, 45.00),
(2, 3, 1, 45.00, 45.00),
(5, 4, 1, 35.00, 35.00);

INSERT INTO venda (id_cliente, data_venda, valor_total, forma_pagamento, observacoes) VALUES
(1, '2026-05-04', 35.00, 'PIX', 'Venda de cabo HDMI junto com retirada do console.'),
(4, '2026-05-05', 210.00, 'CARTAO_DEBITO', 'Venda de controle usado e cabo USB-C.'),
(NULL, '2026-05-08', 30.00, 'DINHEIRO', 'Venda balcao sem cadastro de cliente.');

INSERT INTO item_venda (id_venda, id_produto, quantidade, valor_unitario, subtotal) VALUES
(1, 1, 1, 35.00, 35.00),
(2, 7, 1, 180.00, 180.00),
(2, 9, 1, 30.00, 30.00),
(3, 9, 1, 30.00, 30.00);

INSERT INTO movimentacao_estoque (id_produto, id_usuario, tipo, quantidade, data_movimentacao, motivo, observacoes) VALUES
(1, 1, 'ENTRADA', 20, '2026-05-01 08:00:00', 'Compra inicial de estoque', NULL),
(2, 1, 'ENTRADA', 5, '2026-05-01 08:10:00', 'Compra inicial de estoque', NULL),
(3, 1, 'ENTRADA', 15, '2026-05-01 08:20:00', 'Compra inicial de estoque', NULL),
(4, 1, 'ENTRADA', 12, '2026-05-01 08:30:00', 'Compra inicial de estoque', NULL),
(5, 1, 'ENTRADA', 8, '2026-05-01 08:40:00', 'Compra inicial de estoque', NULL),
(6, 1, 'ENTRADA', 6, '2026-05-01 08:50:00', 'Compra inicial de estoque', NULL),
(7, 1, 'ENTRADA', 4, '2026-05-01 09:00:00', 'Produto usado revisado para venda', NULL),
(8, 1, 'ENTRADA', 3, '2026-05-01 09:10:00', 'Produto usado revisado para venda', NULL),
(9, 1, 'ENTRADA', 18, '2026-05-01 09:20:00', 'Compra inicial de estoque', NULL),
(10, 1, 'ENTRADA', 5, '2026-05-01 09:30:00', 'Compra inicial de estoque', NULL),
(6, 3, 'SAIDA', 1, '2026-05-04 15:00:00', 'Uso em ordem de servico', 'Usado na OS 1'),
(3, 3, 'SAIDA', 1, '2026-05-04 15:05:00', 'Uso em ordem de servico', 'Usado na OS 1'),
(3, 3, 'SAIDA', 1, '2026-05-06 11:00:00', 'Uso em ordem de servico', 'Usado na OS 2'),
(4, 3, 'SAIDA', 1, '2026-05-07 10:00:00', 'Uso em ordem de servico', 'Usado na OS 5'),
(1, 2, 'SAIDA', 1, '2026-05-04 17:10:00', 'Venda de produto', 'Venda 1'),
(7, 2, 'SAIDA', 1, '2026-05-05 14:30:00', 'Venda de produto', 'Venda 2'),
(9, 2, 'SAIDA', 2, '2026-05-08 10:15:00', 'Venda de produto', 'Vendas 2 e 3');

INSERT INTO movimento_caixa (
    id_usuario, id_venda, id_ordem_servico, tipo, descricao, valor,
    data_movimento, forma_pagamento, categoria
) VALUES
(4, NULL, 1, 'ENTRADA', 'Pagamento da OS 1 - Troca de HDMI PS4 Slim', 250.00, '2026-05-04 17:00:00', 'PIX', 'SERVICO'),
(4, 1, NULL, 'ENTRADA', 'Venda 1 - Cabo HDMI', 35.00, '2026-05-04 17:10:00', 'PIX', 'VENDA'),
(4, 2, NULL, 'ENTRADA', 'Venda 2 - Controle PS4 usado e cabo USB-C', 210.00, '2026-05-05 14:30:00', 'CARTAO_DEBITO', 'VENDA'),
(4, NULL, 5, 'ENTRADA', 'Pagamento da OS 5 - Troca de analogico DualShock 4', 90.00, '2026-05-07 10:20:00', 'DINHEIRO', 'SERVICO'),
(4, 3, NULL, 'ENTRADA', 'Venda 3 - Cabo USB-C', 30.00, '2026-05-08 10:15:00', 'DINHEIRO', 'VENDA'),
(4, NULL, NULL, 'SAIDA', 'Compra de pecas para estoque', 850.00, '2026-05-01 08:00:00', 'PIX', 'COMPRA_ESTOQUE'),
(4, NULL, NULL, 'SAIDA', 'Pagamento de internet da loja', 120.00, '2026-05-03 09:00:00', 'PIX', 'DESPESA_FIXA');
