package com.acaogames.backend.service;

import com.acaogames.backend.dto.RelatorioResponse;
import com.acaogames.backend.dto.RelatorioResponse.Column;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    public RelatorioResponse caixa(LocalDate inicio, LocalDate fim, String busca, int page, int size) {
        PageRequest pageRequest = normalizePage(page, size);
        MapSqlParameterSource params = baseParams(inicio, fim, busca, pageRequest);
        String where = " WHERE (:inicio IS NULL OR DATE(mc.data_movimento) >= :inicio) "
                + "AND (:fim IS NULL OR DATE(mc.data_movimento) <= :fim) "
                + "AND (:busca = '' OR LOWER(mc.descricao) LIKE :buscaLike "
                + "OR LOWER(mc.tipo) LIKE :buscaLike "
                + "OR LOWER(mc.categoria) LIKE :buscaLike "
                + "OR LOWER(COALESCE(mc.forma_pagamento, '')) LIKE :buscaLike "
                + "OR CAST(mc.id_movimento_caixa AS TEXT) LIKE :buscaLike)";

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
                + "OR LOWER(v.forma_pagamento) LIKE :buscaLike "
                + "OR LOWER(COALESCE(v.observacoes, '')) LIKE :buscaLike "
                + "OR CAST(v.id_venda AS TEXT) LIKE :buscaLike)";

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
                .addValue("inicio", inicio)
                .addValue("fim", fim)
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
