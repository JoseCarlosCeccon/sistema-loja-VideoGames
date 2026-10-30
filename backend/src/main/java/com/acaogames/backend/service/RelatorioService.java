package com.acaogames.backend.service;

import com.acaogames.backend.dto.RelatorioResponse;
import com.acaogames.backend.dto.RelatorioResponse.Column;
import com.acaogames.backend.exception.BadRequestException;
import com.acaogames.backend.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RelatorioService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RelatorioService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String movimentacoesCsv(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("inicio", inicio == null ? null : inicio.toString())
                .addValue("fim", fim == null ? null : fim.toString());
        String sql = "SELECT origem, tipo, idMovimentacao, data, idVenda, idOrdemServico, idProduto, "
                + "produto, categoriaProduto, categoriaMovimento, quantidadeVendida, quantidadeMovimentada, "
                + "valorUnitario, valorItem, valorMovimento, formaPagamento, cliente, descricao, observacoes "
                + "FROM ("
                + "SELECT 'VENDA' AS origem, COALESCE(mc.tipo, 'ENTRADA') AS tipo, "
                + "mc.id_movimento_caixa AS idMovimentacao, v.data_venda AS data, v.id_venda AS idVenda, "
                + "NULL AS idOrdemServico, p.id_produto AS idProduto, p.nome AS produto, "
                + "p.categoria AS categoriaProduto, COALESCE(mc.categoria, 'VENDA') AS categoriaMovimento, "
                + "iv.quantidade AS quantidadeVendida, NULL AS quantidadeMovimentada, "
                + "iv.valor_unitario AS valorUnitario, iv.subtotal AS valorItem, "
                + "NULL AS valorMovimento, v.forma_pagamento AS formaPagamento, "
                + "COALESCE(c.nome, 'Venda balcao') AS cliente, "
                + "COALESCE(mc.descricao, 'Venda ' || v.id_venda) AS descricao, "
                + "v.observacoes AS observacoes "
                + "FROM venda v "
                + "INNER JOIN item_venda iv ON iv.id_venda = v.id_venda "
                + "LEFT JOIN produto p ON p.id_produto = iv.id_produto "
                + "LEFT JOIN cliente c ON c.id_cliente = v.id_cliente "
                + "LEFT JOIN movimento_caixa mc ON mc.id_venda = v.id_venda "
                + "WHERE (:inicio IS NULL OR DATE(v.data_venda) >= :inicio) "
                + "AND (:fim IS NULL OR DATE(v.data_venda) <= :fim) "
                + "UNION ALL "
                + "SELECT 'CAIXA' AS origem, mc.tipo AS tipo, mc.id_movimento_caixa AS idMovimentacao, "
                + "mc.data_movimento AS data, mc.id_venda AS idVenda, mc.id_ordem_servico AS idOrdemServico, "
                + "NULL AS idProduto, NULL AS produto, NULL AS categoriaProduto, mc.categoria AS categoriaMovimento, "
                + "NULL AS quantidadeVendida, NULL AS quantidadeMovimentada, NULL AS valorUnitario, "
                + "NULL AS valorItem, mc.valor AS valorMovimento, mc.forma_pagamento AS formaPagamento, "
                + "c.nome AS cliente, mc.descricao AS descricao, NULL AS observacoes "
                + "FROM movimento_caixa mc "
                + "LEFT JOIN ordem_servico os ON os.id_ordem_servico = mc.id_ordem_servico "
                + "LEFT JOIN cliente c ON c.id_cliente = os.id_cliente "
                + "WHERE (:inicio IS NULL OR DATE(mc.data_movimento) >= :inicio) "
                + "AND (:fim IS NULL OR DATE(mc.data_movimento) <= :fim) "
                + "UNION ALL "
                + "SELECT 'ESTOQUE' AS origem, me.tipo AS tipo, me.id_movimentacao AS idMovimentacao, "
                + "me.data_movimentacao AS data, NULL AS idVenda, NULL AS idOrdemServico, "
                + "p.id_produto AS idProduto, p.nome AS produto, p.categoria AS categoriaProduto, "
                + "NULL AS categoriaMovimento, NULL AS quantidadeVendida, me.quantidade AS quantidadeMovimentada, "
                + "NULL AS valorUnitario, NULL AS valorItem, NULL AS valorMovimento, NULL AS formaPagamento, "
                + "NULL AS cliente, me.motivo AS descricao, me.observacoes AS observacoes "
                + "FROM movimentacao_estoque me "
                + "INNER JOIN produto p ON p.id_produto = me.id_produto "
                + "WHERE (:inicio IS NULL OR DATE(me.data_movimentacao) >= :inicio) "
                + "AND (:fim IS NULL OR DATE(me.data_movimentacao) <= :fim)"
                + ") ORDER BY data DESC, origem ASC, idMovimentacao DESC, idVenda DESC, idProduto DESC";
        List<String> keys = Arrays.asList(
                "origem", "tipo", "idMovimentacao", "data", "idVenda", "idOrdemServico", "idProduto",
                "produto", "categoriaProduto", "categoriaMovimento", "quantidadeVendida",
                "quantidadeMovimentada", "valorUnitario", "valorItem", "valorMovimento",
                "formaPagamento", "cliente", "descricao", "observacoes"
        );
        List<String> labels = Arrays.asList(
                "Origem", "Tipo", "ID movimentacao", "Data", "ID venda", "ID ordem de servico",
                "ID produto", "Produto", "Categoria do produto", "Categoria da movimentacao",
                "Quantidade vendida", "Quantidade movimentada", "Valor unitario", "Valor do item",
                "Valor da movimentacao", "Forma de pagamento", "Cliente", "Descricao/Motivo", "Observacoes"
        );

        StringBuilder csv = new StringBuilder("\uFEFF");
        appendCsvRow(csv, labels);
        for (Map<String, Object> row : jdbcTemplate.queryForList(sql, params)) {
            List<String> values = new java.util.ArrayList<String>(keys.size());
            for (String key : keys) {
                values.add(csvValue(row.get(key)));
            }
            appendCsvRow(csv, values);
        }
        return csv.toString();
    }

    public RelatorioResponse caixa(LocalDate inicio, LocalDate fim, String busca, int page, int size) {
        PageRequest pageRequest = normalizePage(page, size);
        MapSqlParameterSource params = baseParams(inicio, fim, busca, pageRequest);
        String where = " WHERE (:inicio IS NULL OR DATE(mc.data_movimento) >= :inicio) "
                + "AND (:fim IS NULL OR DATE(mc.data_movimento) <= :fim) "
                + "AND (:busca = '' OR LOWER(mc.descricao) LIKE :buscaLike "
                + "OR LOWER(mc.tipo) LIKE :buscaLike "
                + "OR LOWER(mc.categoria) LIKE :buscaLike "
                + "OR LOWER(COALESCE(mc.forma_pagamento, '')) LIKE :buscaLike "
                + "OR CAST(mc.id_movimento_caixa AS TEXT) LIKE :buscaLike "
                + "OR CAST(mc.id_venda AS TEXT) LIKE :buscaLike "
                + "OR CAST(mc.id_ordem_servico AS TEXT) LIKE :buscaLike "
                + "OR EXISTS (SELECT 1 FROM venda v "
                + "LEFT JOIN cliente cv ON cv.id_cliente = v.id_cliente "
                + "WHERE v.id_venda = mc.id_venda AND ("
                + "LOWER(COALESCE(cv.nome, 'venda balcao')) LIKE :buscaLike "
                + "OR LOWER(COALESCE(cv.telefone, '')) LIKE :buscaLike "
                + "OR LOWER(COALESCE(cv.cpf, '')) LIKE :buscaLike "
                + "OR LOWER(COALESCE(cv.email, '')) LIKE :buscaLike "
                + "OR EXISTS (SELECT 1 FROM item_venda iv "
                + "INNER JOIN produto p ON p.id_produto = iv.id_produto "
                + "WHERE iv.id_venda = v.id_venda AND ("
                + "LOWER(p.nome) LIKE :buscaLike OR LOWER(p.categoria) LIKE :buscaLike "
                + "OR LOWER(COALESCE(p.descricao, '')) LIKE :buscaLike)))) "
                + "OR EXISTS (SELECT 1 FROM ordem_servico os "
                + "INNER JOIN cliente co ON co.id_cliente = os.id_cliente "
                + "INNER JOIN equipamento e ON e.id_equipamento = os.id_equipamento "
                + "WHERE os.id_ordem_servico = mc.id_ordem_servico AND ("
                + "LOWER(co.nome) LIKE :buscaLike "
                + "OR LOWER(e.marca || ' ' || e.tipo_equipamento || ' ' || e.modelo) LIKE :buscaLike "
                + "OR LOWER(COALESCE(e.descricao, '')) LIKE :buscaLike "
                + "OR LOWER(COALESCE(os.numero_serie, '')) LIKE :buscaLike "
                + "OR LOWER(os.problema_relatado) LIKE :buscaLike "
                + "OR LOWER(COALESCE(os.diagnostico, '')) LIKE :buscaLike "
                + "OR LOWER(COALESCE(os.servico_realizado, '')) LIKE :buscaLike)))";

        String rowsSql = "SELECT mc.id_movimento_caixa AS \"id\", mc.data_movimento AS \"data\", "
                + "mc.tipo AS \"tipo\", mc.descricao AS \"descricao\", mc.categoria AS \"categoria\", "
                + "mc.forma_pagamento AS \"formaPagamento\", mc.valor AS \"valor\", "
                + "mc.id_venda AS \"idVenda\", mc.id_ordem_servico AS \"idOrdemServico\" "
                + "FROM movimento_caixa mc " + where
                + " ORDER BY mc.data_movimento DESC, mc.id_movimento_caixa DESC LIMIT :limit OFFSET :offset";

        String summarySql = "SELECT "
                + "COALESCE(SUM(CASE WHEN mc.tipo = 'ENTRADA' THEN mc.valor ELSE 0 END), 0) AS \"valorEntradas\", "
                + "COALESCE(SUM(CASE WHEN mc.tipo = 'SAIDA' THEN mc.valor ELSE 0 END), 0) AS \"valorSaidas\", "
                + "COALESCE(SUM(CASE WHEN mc.tipo = 'ENTRADA' THEN mc.valor ELSE -mc.valor END), 0) AS saldo "
                + "FROM movimento_caixa mc " + where;

        RelatorioResponse response = buildResponse("caixa", pageRequest, count("movimento_caixa mc", where, params));
        response.setColumns(Arrays.asList(
                column("id", "Mov.", "number"),
                column("data", "Data", "datetime"),
                column("tipo", "Tipo", "text"),
                column("descricao", "Descricao", "text"),
                column("categoria", "Categoria", "text"),
                column("formaPagamento", "Pagamento", "text"),
                column("valor", "Valor", "money")
        ));
        response.setRows(jdbcTemplate.queryForList(rowsSql, params));
        response.setSummary(jdbcTemplate.queryForMap(summarySql, params));
        return response;
    }

    public RelatorioResponse vendas(LocalDate inicio, LocalDate fim, String busca, int page, int size) {
        PageRequest pageRequest = normalizePage(page, size);
        MapSqlParameterSource params = baseParams(inicio, fim, busca, pageRequest);
        String where = " WHERE (:inicio IS NULL OR v.data_venda >= :inicio) "
                + "AND (:fim IS NULL OR v.data_venda <= :fim) "
                + "AND (:busca = '' OR LOWER(COALESCE(c.nome, 'venda balcao')) LIKE :buscaLike "
                + "OR LOWER(COALESCE(c.telefone, '')) LIKE :buscaLike "
                + "OR LOWER(COALESCE(c.cpf, '')) LIKE :buscaLike "
                + "OR LOWER(COALESCE(c.email, '')) LIKE :buscaLike "
                + "OR LOWER(v.forma_pagamento) LIKE :buscaLike "
                + "OR LOWER(COALESCE(v.observacoes, '')) LIKE :buscaLike "
                + "OR CAST(v.id_venda AS TEXT) LIKE :buscaLike "
                + "OR EXISTS (SELECT 1 FROM item_venda iv_busca "
                + "INNER JOIN produto p_busca ON p_busca.id_produto = iv_busca.id_produto "
                + "WHERE iv_busca.id_venda = v.id_venda AND ("
                + "LOWER(p_busca.nome) LIKE :buscaLike "
                + "OR LOWER(p_busca.categoria) LIKE :buscaLike "
                + "OR LOWER(COALESCE(p_busca.descricao, '')) LIKE :buscaLike "
                + "OR CAST(p_busca.id_produto AS TEXT) LIKE :buscaLike "
                + "OR CAST(iv_busca.id_item_venda AS TEXT) LIKE :buscaLike)))";

        String from = "venda v LEFT JOIN cliente c ON c.id_cliente = v.id_cliente ";
        String rowsSql = "SELECT v.id_venda AS \"id\", v.data_venda AS \"data\", "
                + "COALESCE(c.nome, 'Venda balcao') AS \"cliente\", v.forma_pagamento AS \"formaPagamento\", "
                + "v.valor_total AS \"total\", COUNT(iv.id_item_venda) AS \"itens\", v.observacoes AS \"observacoes\" "
                + "FROM " + from
                + "LEFT JOIN item_venda iv ON iv.id_venda = v.id_venda "
                + where
                + " GROUP BY v.id_venda, v.data_venda, c.nome, v.forma_pagamento, v.valor_total, v.observacoes "
                + "ORDER BY v.data_venda DESC, v.id_venda DESC LIMIT :limit OFFSET :offset";

        String summarySql = "SELECT COUNT(*) AS vendas, COALESCE(SUM(v.valor_total), 0) AS \"valorTotal\", "
                + "COALESCE(AVG(v.valor_total), 0) AS \"ticketMedio\" FROM " + from + where;

        RelatorioResponse response = buildResponse("vendas", pageRequest, count(from, where, params));
        response.setColumns(Arrays.asList(
                column("id", "Venda", "number"),
                column("data", "Data", "date"),
                column("cliente", "Cliente", "text"),
                column("formaPagamento", "Pagamento", "text"),
                column("itens", "Itens", "number"),
                column("total", "Total", "money")
        ));
        response.setRows(jdbcTemplate.queryForList(rowsSql, params));
        response.setSummary(jdbcTemplate.queryForMap(summarySql, params));
        return response;
    }

    public RelatorioResponse ordens(LocalDate inicio, LocalDate fim, String busca, int page, int size) {
        PageRequest pageRequest = normalizePage(page, size);
        MapSqlParameterSource params = baseParams(inicio, fim, busca, pageRequest);
        String from = "ordem_servico os "
                + "INNER JOIN cliente c ON c.id_cliente = os.id_cliente "
                + "INNER JOIN equipamento e ON e.id_equipamento = os.id_equipamento ";
        String where = " WHERE (:inicio IS NULL OR os.data_entrada >= :inicio) "
                + "AND (:fim IS NULL OR os.data_entrada <= :fim) "
                + "AND (:busca = '' OR LOWER(c.nome) LIKE :buscaLike "
                + "OR LOWER(e.marca || ' ' || e.tipo_equipamento || ' ' || e.modelo) LIKE :buscaLike "
                + "OR LOWER(os.status) LIKE :buscaLike "
                + "OR LOWER(os.problema_relatado) LIKE :buscaLike "
                + "OR CAST(os.id_ordem_servico AS TEXT) LIKE :buscaLike)";

        String rowsSql = "SELECT os.id_ordem_servico AS \"id\", os.data_entrada AS \"dataEntrada\", "
                + "c.nome AS \"cliente\", e.marca || ' ' || e.modelo AS \"equipamento\", "
                + "os.status AS \"status\", os.problema_relatado AS \"problema\", "
                + "os.valor_servico AS \"valorServico\" "
                + "FROM " + from + where
                + " ORDER BY os.data_entrada DESC, os.id_ordem_servico DESC LIMIT :limit OFFSET :offset";

        String summarySql = "SELECT COUNT(*) AS ordens, "
                + "COALESCE(SUM(CASE WHEN os.status NOT IN ('ENTREGUE', 'CANCELADO') THEN 1 ELSE 0 END), 0) AS abertas, "
                + "COALESCE(SUM(CASE WHEN os.status = 'PRONTO' THEN 1 ELSE 0 END), 0) AS prontas, "
                + "COALESCE(SUM(CASE WHEN os.status = 'ENTREGUE' THEN 1 ELSE 0 END), 0) AS entregues, "
                + "COALESCE(SUM(os.valor_servico), 0) AS \"valorServicos\" "
                + "FROM " + from + where;

        RelatorioResponse response = buildResponse("ordens", pageRequest, count(from, where, params));
        response.setColumns(Arrays.asList(
                column("id", "OS", "number"),
                column("dataEntrada", "Entrada", "date"),
                column("cliente", "Cliente", "text"),
                column("equipamento", "Equipamento", "text"),
                column("status", "Status", "text"),
                column("problema", "Problema", "text"),
                column("valorServico", "Valor", "money")
        ));
        response.setRows(jdbcTemplate.queryForList(rowsSql, params));
        response.setSummary(jdbcTemplate.queryForMap(summarySql, params));
        return response;
    }

    public RelatorioResponse estoque(LocalDate inicio, LocalDate fim, String busca, int page, int size) {
        PageRequest pageRequest = normalizePage(page, size);
        MapSqlParameterSource params = baseParams(inicio, fim, busca, pageRequest);
        String where = " WHERE (:busca = '' OR LOWER(p.nome) LIKE :buscaLike "
                + "OR LOWER(p.categoria) LIKE :buscaLike "
                + "OR LOWER(COALESCE(p.descricao, '')) LIKE :buscaLike "
                + "OR CAST(p.id_produto AS TEXT) LIKE :buscaLike)";

        String rowsSql = "SELECT p.id_produto AS \"id\", p.nome AS \"produto\", p.categoria AS \"categoria\", "
                + "p.quantidade_estoque AS \"quantidade\", p.estoque_minimo AS \"estoqueMinimo\", "
                + "p.valor_venda AS \"valorVenda\", "
                + "CASE WHEN p.quantidade_estoque <= p.estoque_minimo THEN 'BAIXO' ELSE 'OK' END AS \"status\" "
                + "FROM produto p " + where
                + " ORDER BY p.nome LIMIT :limit OFFSET :offset";

        String summarySql = "SELECT COUNT(*) AS produtos, "
                + "COALESCE(SUM(CASE WHEN p.quantidade_estoque <= p.estoque_minimo THEN 1 ELSE 0 END), 0) AS \"baixoEstoque\", "
                + "COALESCE(SUM(p.quantidade_estoque * p.valor_venda), 0) AS \"valorEstoque\" "
                + "FROM produto p " + where;

        Map<String, Object> summary = new LinkedHashMap<String, Object>(jdbcTemplate.queryForMap(summarySql, params));
        summary.putAll(movimentacoesEstoqueResumo(inicio, fim, busca));

        RelatorioResponse response = buildResponse("estoque", pageRequest, count("produto p", where, params));
        response.setColumns(Arrays.asList(
                column("id", "Produto", "number"),
                column("produto", "Nome", "text"),
                column("categoria", "Categoria", "text"),
                column("quantidade", "Qtd.", "number"),
                column("estoqueMinimo", "Min.", "number"),
                column("valorVenda", "Venda", "money"),
                column("status", "Status", "text")
        ));
        response.setRows(jdbcTemplate.queryForList(rowsSql, params));
        response.setSummary(summary);
        return response;
    }

    public RelatorioResponse clientes(LocalDate inicio, LocalDate fim, String busca, int page, int size) {
        PageRequest pageRequest = normalizePage(page, size);
        MapSqlParameterSource params = baseParams(inicio, fim, busca, pageRequest);
        String where = " WHERE (:busca = '' OR LOWER(c.nome) LIKE :buscaLike "
                + "OR LOWER(c.telefone) LIKE :buscaLike "
                + "OR LOWER(COALESCE(c.email, '')) LIKE :buscaLike "
                + "OR LOWER(COALESCE(c.cpf, '')) LIKE :buscaLike)";

        String rowsSql = "SELECT c.id_cliente AS \"id\", c.nome AS \"cliente\", c.telefone AS \"telefone\", "
                + "c.email AS \"email\", c.ativo AS \"ativo\", "
                + "COUNT(DISTINCT os.id_ordem_servico) AS \"ordens\", COUNT(DISTINCT v.id_venda) AS \"vendas\", "
                + "COALESCE(SUM(DISTINCT v.valor_total), 0) AS \"totalGasto\" "
                + "FROM cliente c "
                + "LEFT JOIN ordem_servico os ON os.id_cliente = c.id_cliente "
                + "AND (:inicio IS NULL OR os.data_entrada >= :inicio) AND (:fim IS NULL OR os.data_entrada <= :fim) "
                + "LEFT JOIN venda v ON v.id_cliente = c.id_cliente "
                + "AND (:inicio IS NULL OR v.data_venda >= :inicio) AND (:fim IS NULL OR v.data_venda <= :fim) "
                + where
                + " GROUP BY c.id_cliente, c.nome, c.telefone, c.email, c.ativo "
                + "ORDER BY c.nome LIMIT :limit OFFSET :offset";

        String summarySql = "SELECT COUNT(*) AS clientes, "
                + "COALESCE(SUM(CASE WHEN c.ativo THEN 1 ELSE 0 END), 0) AS ativos "
                + "FROM cliente c " + where;

        RelatorioResponse response = buildResponse("clientes", pageRequest, count("cliente c", where, params));
        response.setColumns(Arrays.asList(
                column("id", "Cliente", "number"),
                column("cliente", "Nome", "text"),
                column("telefone", "Telefone", "text"),
                column("email", "Email", "text"),
                column("ativo", "Ativo", "boolean"),
                column("ordens", "OS", "number"),
                column("vendas", "Vendas", "number"),
                column("totalGasto", "Total", "money")
        ));
        response.setRows(jdbcTemplate.queryForList(rowsSql, params));
        response.setSummary(jdbcTemplate.queryForMap(summarySql, params));
        return response;
    }

    public RelatorioResponse produtos(
            LocalDate inicio,
            LocalDate fim,
            String busca,
            Long idProduto,
            String categoria,
            String movimentacao,
            String sortBy,
            String sortOrder,
            int page,
            int size
    ) {
        validarPeriodo(inicio, fim);
        PageRequest pageRequest = normalizePage(page, size);
        String movimentoNormalizado = normalizarMovimentacao(movimentacao);
        String campoOrdenacao = normalizarCampoOrdenacao(sortBy);
        String direcaoOrdenacao = normalizarDirecaoOrdenacao(sortOrder);
        MapSqlParameterSource params = produtoParams(
                inicio, fim, busca, idProduto, categoria, movimentoNormalizado, pageRequest
        );
        String cte = produtoCte();
        String orderBy = produtoOrderBy(campoOrdenacao, direcaoOrdenacao);

        Number total = jdbcTemplate.queryForObject(
                cte + " SELECT COUNT(*) FROM produtos_filtrados",
                params,
                Number.class
        );
        RelatorioResponse response = buildResponse(
                "produtos",
                pageRequest,
                total == null ? 0L : total.longValue()
        );
        response.setColumns(Arrays.asList(
                column("produto", "Produto", "text"),
                column("categoria", "Categoria", "text"),
                column("unidadesVendidas", "Unidades Vendidas", "number"),
                column("numeroVendas", "Nº de Vendas", "number"),
                column("precoMedio", "Preço Médio", "money"),
                column("faturamento", "Faturamento", "money")
        ));
        response.setRows(jdbcTemplate.queryForList(
                cte
                        + " SELECT id_produto AS \"id\", produto AS \"produto\", categoria AS \"categoria\", "
                        + "unidades_vendidas AS \"unidadesVendidas\", numero_vendas AS \"numeroVendas\", "
                        + "preco_medio AS \"precoMedio\", faturamento AS \"faturamento\", "
                        + "estoque_atual AS \"estoqueAtual\", ultima_venda AS \"ultimaVenda\" "
                        + "FROM produtos_filtrados ORDER BY " + orderBy + " LIMIT :limit OFFSET :offset",
                params
        ));

        Map<String, Object> resumoAtual = produtoResumo(params);
        response.setSummary(resumoAtual);
        response.setRankings(produtoRankings(cte, params));

        if (inicio != null && fim != null) {
            long quantidadeDias = ChronoUnit.DAYS.between(inicio, fim) + 1L;
            LocalDate fimAnterior = inicio.minusDays(1L);
            LocalDate inicioAnterior = fimAnterior.minusDays(quantidadeDias - 1L);
            MapSqlParameterSource paramsAnteriores = produtoParams(
                    inicioAnterior,
                    fimAnterior,
                    busca,
                    idProduto,
                    categoria,
                    movimentoNormalizado,
                    new PageRequest(0, 1)
            );
            response.setComparison(compararResumos(
                    resumoAtual,
                    produtoResumo(paramsAnteriores),
                    inicioAnterior,
                    fimAnterior
            ));
        }
        return response;
    }

    public RelatorioResponse produtoVendas(
            Long idProduto,
            LocalDate inicio,
            LocalDate fim,
            int page,
            int size
    ) {
        validarPeriodo(inicio, fim);
        PageRequest pageRequest = normalizePage(page, size);
        MapSqlParameterSource params = baseParams(inicio, fim, "", pageRequest)
                .addValue("idProduto", idProduto);
        List<Map<String, Object>> produtos = jdbcTemplate.queryForList(
                "SELECT id_produto AS \"id\", nome AS \"produto\", categoria AS \"categoria\", "
                        + "quantidade_estoque AS \"estoqueAtual\" FROM produto WHERE id_produto = :idProduto",
                params
        );
        if (produtos.isEmpty()) {
            throw new ResourceNotFoundException("Produto nao encontrado.");
        }

        String where = " WHERE iv.id_produto = :idProduto "
                + "AND (:inicio IS NULL OR v.data_venda >= :inicio) "
                + "AND (:fim IS NULL OR v.data_venda <= :fim) "
                + "AND EXISTS (SELECT 1 FROM movimento_caixa mc "
                + "WHERE mc.id_venda = v.id_venda AND mc.tipo = 'ENTRADA') ";
        String from = "item_venda iv INNER JOIN venda v ON v.id_venda = iv.id_venda "
                + "LEFT JOIN cliente c ON c.id_cliente = v.id_cliente ";
        Number total = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT v.id_venda) FROM " + from + where,
                params,
                Number.class
        );
        RelatorioResponse response = buildResponse(
                "produto-vendas",
                pageRequest,
                total == null ? 0L : total.longValue()
        );
        response.setColumns(Arrays.asList(
                column("id", "Venda", "number"),
                column("data", "Data", "date"),
                column("cliente", "Cliente", "text"),
                column("quantidade", "Quantidade", "number"),
                column("precoUnitario", "Preço Unitário", "money"),
                column("total", "Total", "money")
        ));
        response.setRows(jdbcTemplate.queryForList(
                "SELECT v.id_venda AS \"id\", v.data_venda AS \"data\", "
                        + "COALESCE(c.nome, 'Venda balcao') AS \"cliente\", "
                        + "SUM(iv.quantidade) AS \"quantidade\", "
                        + "CASE WHEN SUM(iv.quantidade) = 0 THEN 0 "
                        + "ELSE SUM(iv.subtotal) / SUM(iv.quantidade) END AS \"precoUnitario\", "
                        + "SUM(iv.subtotal) AS \"total\" FROM " + from + where
                        + "GROUP BY v.id_venda, v.data_venda, c.nome "
                        + "ORDER BY v.data_venda DESC, v.id_venda DESC LIMIT :limit OFFSET :offset",
                params
        ));

        Map<String, Object> summary = new LinkedHashMap<String, Object>(produtos.get(0));
        summary.putAll(jdbcTemplate.queryForMap(
                "SELECT COALESCE(SUM(iv.quantidade), 0) AS \"unidadesVendidas\", "
                        + "COUNT(DISTINCT v.id_venda) AS \"vendasRealizadas\", "
                        + "COALESCE(SUM(iv.subtotal), 0) AS \"faturamento\", "
                        + "CASE WHEN COALESCE(SUM(iv.quantidade), 0) = 0 THEN 0 "
                        + "ELSE SUM(iv.subtotal) / SUM(iv.quantidade) END AS \"precoMedio\", "
                        + "MAX(v.data_venda) AS \"ultimaVenda\" FROM " + from + where,
                params
        ));
        response.setSummary(summary);
        return response;
    }

    private String produtoCte() {
        return "WITH desempenho AS ("
                + "SELECT iv.id_produto, SUM(iv.quantidade) AS unidades_vendidas, "
                + "COUNT(DISTINCT iv.id_venda) AS numero_vendas, "
                + "SUM(iv.subtotal) AS faturamento, MAX(v.data_venda) AS ultima_venda "
                + "FROM item_venda iv INNER JOIN venda v ON v.id_venda = iv.id_venda "
                + "WHERE (:inicio IS NULL OR v.data_venda >= :inicio) "
                + "AND (:fim IS NULL OR v.data_venda <= :fim) "
                + "AND EXISTS (SELECT 1 FROM movimento_caixa mc "
                + "WHERE mc.id_venda = v.id_venda AND mc.tipo = 'ENTRADA') "
                + "GROUP BY iv.id_produto), "
                + "produtos_filtrados AS ("
                + "SELECT p.id_produto, p.nome AS produto, p.categoria, p.quantidade_estoque AS estoque_atual, "
                + "COALESCE(d.unidades_vendidas, 0) AS unidades_vendidas, "
                + "COALESCE(d.numero_vendas, 0) AS numero_vendas, "
                + "CASE WHEN COALESCE(d.unidades_vendidas, 0) = 0 THEN 0 "
                + "ELSE d.faturamento / d.unidades_vendidas END AS preco_medio, "
                + "COALESCE(d.faturamento, 0) AS faturamento, d.ultima_venda "
                + "FROM produto p LEFT JOIN desempenho d ON d.id_produto = p.id_produto "
                + "WHERE (:busca = '' OR LOWER(p.nome) LIKE :buscaLike "
                + "OR LOWER(p.categoria) LIKE :buscaLike "
                + "OR LOWER(COALESCE(p.descricao, '')) LIKE :buscaLike "
                + "OR CAST(p.id_produto AS TEXT) LIKE :buscaLike) "
                + "AND (:idProduto IS NULL OR p.id_produto = :idProduto) "
                + "AND (:categoria = '' OR LOWER(p.categoria) = :categoria) "
                + "AND (:movimentacao = 'TODOS' "
                + "OR (:movimentacao = 'COM_VENDAS' AND COALESCE(d.unidades_vendidas, 0) > 0) "
                + "OR (:movimentacao = 'SEM_VENDAS' AND p.ativo = 1 AND p.quantidade_estoque > 0 "
                + "AND COALESCE(d.unidades_vendidas, 0) = 0))) ";
    }

    private Map<String, Object> produtoResumo(MapSqlParameterSource params) {
        String sql = produtoCte()
                + "SELECT COALESCE(SUM(pf.unidades_vendidas), 0) AS \"unidadesVendidas\", "
                + "(SELECT COUNT(DISTINCT iv.id_venda) FROM item_venda iv "
                + "INNER JOIN venda v ON v.id_venda = iv.id_venda "
                + "INNER JOIN produtos_filtrados pfx ON pfx.id_produto = iv.id_produto "
                + "WHERE (:inicio IS NULL OR v.data_venda >= :inicio) "
                + "AND (:fim IS NULL OR v.data_venda <= :fim) "
                + "AND EXISTS (SELECT 1 FROM movimento_caixa mc "
                + "WHERE mc.id_venda = v.id_venda AND mc.tipo = 'ENTRADA')) AS \"vendasRealizadas\", "
                + "COALESCE(SUM(pf.faturamento), 0) AS \"faturamentoProdutos\", "
                + "COALESCE(SUM(CASE WHEN pf.unidades_vendidas > 0 THEN 1 ELSE 0 END), 0) "
                + "AS \"produtosDiferentes\" FROM produtos_filtrados pf";
        return jdbcTemplate.queryForMap(sql, params);
    }

    private Map<String, List<Map<String, Object>>> produtoRankings(
            String cte,
            MapSqlParameterSource params
    ) {
        Map<String, List<Map<String, Object>>> rankings =
                new LinkedHashMap<String, List<Map<String, Object>>>();
        rankings.put("maisVendidos", jdbcTemplate.queryForList(
                cte + " SELECT id_produto AS \"id\", produto AS \"produto\", "
                        + "unidades_vendidas AS \"valor\" FROM produtos_filtrados "
                        + "WHERE unidades_vendidas > 0 "
                        + "ORDER BY unidades_vendidas DESC, produto ASC LIMIT 5",
                params
        ));
        rankings.put("maiorFaturamento", jdbcTemplate.queryForList(
                cte + " SELECT id_produto AS \"id\", produto AS \"produto\", "
                        + "faturamento AS \"valor\" FROM produtos_filtrados "
                        + "WHERE faturamento > 0 "
                        + "ORDER BY faturamento DESC, produto ASC LIMIT 5",
                params
        ));
        return rankings;
    }

    private Map<String, Object> compararResumos(
            Map<String, Object> atual,
            Map<String, Object> anterior,
            LocalDate inicioAnterior,
            LocalDate fimAnterior
    ) {
        Map<String, Object> comparison = new LinkedHashMap<String, Object>();
        comparison.put("inicioAnterior", inicioAnterior.toString());
        comparison.put("fimAnterior", fimAnterior.toString());
        for (String metrica : Arrays.asList(
                "unidadesVendidas",
                "vendasRealizadas",
                "faturamentoProdutos",
                "produtosDiferentes"
        )) {
            BigDecimal valorAtual = decimal(atual.get(metrica));
            BigDecimal valorAnterior = decimal(anterior.get(metrica));
            Map<String, Object> comparacao = new LinkedHashMap<String, Object>();
            comparacao.put("valorAnterior", valorAnterior);
            if (valorAnterior.compareTo(BigDecimal.ZERO) == 0) {
                comparacao.put("disponivel", false);
            } else {
                BigDecimal percentual = valorAtual.subtract(valorAnterior)
                        .divide(valorAnterior.abs(), 6, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(1, RoundingMode.HALF_UP);
                comparacao.put("disponivel", true);
                comparacao.put("percentual", percentual);
                comparacao.put("direcao", percentual.signum() > 0 ? "UP" : percentual.signum() < 0 ? "DOWN" : "SAME");
            }
            comparison.put(metrica, comparacao);
        }
        return comparison;
    }

    private MapSqlParameterSource produtoParams(
            LocalDate inicio,
            LocalDate fim,
            String busca,
            Long idProduto,
            String categoria,
            String movimentacao,
            PageRequest pageRequest
    ) {
        return baseParams(inicio, fim, busca, pageRequest)
                .addValue("idProduto", idProduto)
                .addValue("categoria", categoria == null ? "" : categoria.trim().toLowerCase())
                .addValue("movimentacao", movimentacao);
    }

    private String normalizarMovimentacao(String movimentacao) {
        String value = movimentacao == null ? "TODOS" : movimentacao.trim().toUpperCase();
        if (!Arrays.asList("TODOS", "COM_VENDAS", "SEM_VENDAS").contains(value)) {
            throw new BadRequestException("Filtro de movimentacao invalido.");
        }
        return value;
    }

    private String normalizarCampoOrdenacao(String sortBy) {
        String value = sortBy == null ? "unidadesvendidas" : sortBy.trim().toLowerCase();
        if ("produto".equals(value)) {
            return "produto";
        }
        if ("categoria".equals(value)) {
            return "categoria";
        }
        if ("unidadesvendidas".equals(value)) {
            return "unidadesVendidas";
        }
        if ("numerovendas".equals(value)) {
            return "numeroVendas";
        }
        if ("precomedio".equals(value)) {
            return "precoMedio";
        }
        if ("faturamento".equals(value)) {
            return "faturamento";
        }
        throw new BadRequestException("Campo de ordenacao de produtos invalido.");
    }

    private String normalizarDirecaoOrdenacao(String sortOrder) {
        String value = sortOrder == null ? "desc" : sortOrder.trim().toLowerCase();
        if (!"asc".equals(value) && !"desc".equals(value)) {
            throw new BadRequestException("Direcao de ordenacao invalida.");
        }
        return value.toUpperCase();
    }

    private String produtoOrderBy(String sortBy, String sortOrder) {
        String productTieBreaker = "produto COLLATE NOCASE ASC, id_produto DESC";
        if ("produto".equals(sortBy)) {
            return "produto COLLATE NOCASE " + sortOrder + ", id_produto DESC";
        }
        if ("categoria".equals(sortBy)) {
            return "categoria COLLATE NOCASE " + sortOrder + ", " + productTieBreaker;
        }
        if ("numeroVendas".equals(sortBy)) {
            return "numero_vendas " + sortOrder + ", " + productTieBreaker;
        }
        if ("precoMedio".equals(sortBy)) {
            return "preco_medio " + sortOrder + ", " + productTieBreaker;
        }
        if ("faturamento".equals(sortBy)) {
            return "faturamento " + sortOrder + ", " + productTieBreaker;
        }
        return "unidades_vendidas " + sortOrder + ", " + productTieBreaker;
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new BadRequestException("A data inicial nao pode ser posterior a data final.");
        }
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString());
    }

    private Map<String, Object> movimentacoesEstoqueResumo(LocalDate inicio, LocalDate fim, String busca) {
        MapSqlParameterSource params = baseParams(inicio, fim, busca, new PageRequest(0, 1));
        String sql = "SELECT "
                + "COALESCE(SUM(CASE WHEN me.tipo = 'ENTRADA' THEN me.quantidade ELSE 0 END), 0) AS \"entradasPeriodo\", "
                + "COALESCE(SUM(CASE WHEN me.tipo = 'SAIDA' THEN me.quantidade ELSE 0 END), 0) AS \"saidasPeriodo\" "
                + "FROM movimentacao_estoque me "
                + "INNER JOIN produto p ON p.id_produto = me.id_produto "
                + "WHERE (:inicio IS NULL OR DATE(me.data_movimentacao) >= :inicio) "
                + "AND (:fim IS NULL OR DATE(me.data_movimentacao) <= :fim) "
                + "AND (:busca = '' OR LOWER(p.nome) LIKE :buscaLike OR LOWER(me.motivo) LIKE :buscaLike)";
        return jdbcTemplate.queryForMap(sql, params);
    }

    private RelatorioResponse buildResponse(String tipo, PageRequest pageRequest, long totalElements) {
        RelatorioResponse response = new RelatorioResponse();
        response.setTipo(tipo);
        response.setPage(pageRequest.getPage());
        response.setSize(pageRequest.getSize());
        response.setTotalElements(totalElements);
        response.setTotalPages((int) Math.ceil((double) totalElements / pageRequest.getSize()));
        return response;
    }

    private long count(String from, String where, MapSqlParameterSource params) {
        Number total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + from + where, params, Number.class);
        return total == null ? 0L : total.longValue();
    }

    private MapSqlParameterSource baseParams(LocalDate inicio, LocalDate fim, String busca, PageRequest pageRequest) {
        String normalizedSearch = busca == null ? "" : busca.trim().toLowerCase();
        return new MapSqlParameterSource()
                .addValue("inicio", inicio == null ? null : inicio.toString())
                .addValue("fim", fim == null ? null : fim.toString())
                .addValue("busca", normalizedSearch)
                .addValue("buscaLike", "%" + normalizedSearch + "%")
                .addValue("limit", pageRequest.getSize())
                .addValue("offset", pageRequest.getPage() * pageRequest.getSize());
    }

    private PageRequest normalizePage(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size <= 0 ? 20 : size, MAX_PAGE_SIZE));
        return new PageRequest(safePage, safeSize);
    }

    private Column column(String key, String label, String type) {
        return new Column(key, label, type);
    }

    private void appendCsvRow(StringBuilder csv, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                csv.append(';');
            }
            csv.append('"').append(values.get(index).replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (value instanceof String) {
            int firstNonWhitespace = 0;
            while (firstNonWhitespace < text.length() && Character.isWhitespace(text.charAt(firstNonWhitespace))) {
                firstNonWhitespace++;
            }
            if (firstNonWhitespace < text.length()
                    && "=+-@\t".indexOf(text.charAt(firstNonWhitespace)) >= 0) {
                text = "'" + text;
            }
        }
        return text;
    }

    private static class PageRequest {
        private final int page;
        private final int size;

        PageRequest(int page, int size) {
            this.page = page;
            this.size = size;
        }

        int getPage() {
            return page;
        }

        int getSize() {
            return size;
        }
    }
}
