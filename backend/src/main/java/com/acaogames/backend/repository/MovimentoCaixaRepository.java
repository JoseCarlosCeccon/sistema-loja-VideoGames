package com.acaogames.backend.repository;

import com.acaogames.backend.model.MovimentoCaixa;
import com.acaogames.backend.model.OrdemServico;
import com.acaogames.backend.model.UsuarioSistema;
import com.acaogames.backend.model.Venda;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class MovimentoCaixaRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MovimentoCaixaRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MovimentoCaixa> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM movimento_caixa ORDER BY data_movimento DESC, id_movimento_caixa DESC",
                rowMapper()
        );
    }

    public Optional<MovimentoCaixa> findById(Long idMovimentoCaixa) {
        List<MovimentoCaixa> movimentos = jdbcTemplate.query(
                "SELECT * FROM movimento_caixa WHERE id_movimento_caixa = :idMovimentoCaixa",
                new MapSqlParameterSource("idMovimentoCaixa", idMovimentoCaixa),
                rowMapper()
        );
        return movimentos.stream().findFirst();
    }

    public Optional<MovimentoCaixa> findByVendaId(Long idVenda) {
        List<MovimentoCaixa> movimentos = jdbcTemplate.query(
                "SELECT * FROM movimento_caixa WHERE id_venda = :idVenda",
                new MapSqlParameterSource("idVenda", idVenda),
                rowMapper()
        );
        return movimentos.stream().findFirst();
    }

    public MovimentoCaixa create(MovimentoCaixa movimento) {
        String sql = "INSERT INTO movimento_caixa(id_usuario, id_venda, id_ordem_servico, tipo, descricao, "
                + "valor, data_movimento, forma_pagamento, categoria) VALUES(:idUsuario, :idVenda, "
                + ":idOrdemServico, :tipo, :descricao, :valor, :dataMovimento, :formaPagamento, :categoria)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, toParams(movimento), keyHolder, new String[]{"id_movimento_caixa"});
        Number key = keyHolder.getKey();
        if (key != null) {
            movimento.setIdMovimentoCaixa(key.longValue());
        }
        return findById(movimento.getIdMovimentoCaixa()).orElse(movimento);
    }

    public MovimentoCaixa update(MovimentoCaixa movimento) {
        String sql = "UPDATE movimento_caixa SET id_usuario = :idUsuario, id_venda = :idVenda, "
                + "id_ordem_servico = :idOrdemServico, tipo = :tipo, descricao = :descricao, valor = :valor, "
                + "data_movimento = :dataMovimento, forma_pagamento = :formaPagamento, categoria = :categoria "
                + "WHERE id_movimento_caixa = :idMovimentoCaixa";
        MapSqlParameterSource params = toParams(movimento);
        params.addValue("idMovimentoCaixa", movimento.getIdMovimentoCaixa());
        jdbcTemplate.update(sql, params);
        return findById(movimento.getIdMovimentoCaixa()).orElse(movimento);
    }

    public void delete(Long idMovimentoCaixa) {
        jdbcTemplate.update(
                "DELETE FROM movimento_caixa WHERE id_movimento_caixa = :idMovimentoCaixa",
                new MapSqlParameterSource("idMovimentoCaixa", idMovimentoCaixa)
        );
    }

    public void deleteByVendaId(Long idVenda) {
        jdbcTemplate.update(
                "DELETE FROM movimento_caixa WHERE id_venda = :idVenda",
                new MapSqlParameterSource("idVenda", idVenda)
        );
    }

    public void deleteByOrdemServicoId(Long idOrdemServico) {
        jdbcTemplate.update(
                "DELETE FROM movimento_caixa WHERE id_ordem_servico = :idOrdemServico",
                new MapSqlParameterSource("idOrdemServico", idOrdemServico)
        );
    }

    public BigDecimal saldo() {
        BigDecimal total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(CASE WHEN tipo = 'ENTRADA' THEN valor ELSE -valor END), 0) FROM movimento_caixa",
                new MapSqlParameterSource(),
                BigDecimal.class
        );
        return total == null ? BigDecimal.ZERO : total;
    }

    public BigDecimal saldoPorData(LocalDate data) {
        BigDecimal total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(CASE WHEN tipo = 'ENTRADA' THEN valor ELSE -valor END), 0) "
                        + "FROM movimento_caixa WHERE DATE(data_movimento) = :data",
                new MapSqlParameterSource("data", JdbcTemporalValues.toDatabaseValue(data)),
                BigDecimal.class
        );
        return total == null ? BigDecimal.ZERO : total;
    }

    private MapSqlParameterSource toParams(MovimentoCaixa movimento) {
        Long idVenda = movimento.getVenda() == null ? null : movimento.getVenda().getIdVenda();
        Long idOrdemServico = movimento.getOrdemServico() == null ? null : movimento.getOrdemServico().getIdOrdemServico();
        return new MapSqlParameterSource()
                .addValue("idUsuario", movimento.getUsuario().getIdUsuario())
                .addValue("idVenda", idVenda)
                .addValue("idOrdemServico", idOrdemServico)
                .addValue("tipo", movimento.getTipo())
                .addValue("descricao", movimento.getDescricao())
                .addValue("valor", movimento.getValor())
                .addValue("dataMovimento", JdbcTemporalValues.toDatabaseValue(movimento.getDataMovimento()))
                .addValue("formaPagamento", movimento.getFormaPagamento())
                .addValue("categoria", movimento.getCategoria());
    }

    private RowMapper<MovimentoCaixa> rowMapper() {
        return new RowMapper<MovimentoCaixa>() {
            @Override
            public MovimentoCaixa mapRow(ResultSet rs, int rowNum) throws SQLException {
                UsuarioSistema usuario = new UsuarioSistema();
                usuario.setIdUsuario(rs.getLong("id_usuario"));

                MovimentoCaixa movimento = new MovimentoCaixa();
                movimento.setIdMovimentoCaixa(rs.getLong("id_movimento_caixa"));
                movimento.setUsuario(usuario);
                Long idVenda = nullableLong(rs, "id_venda");
                if (idVenda != null) {
                    Venda venda = new Venda();
                    venda.setIdVenda(idVenda);
                    movimento.setVenda(venda);
                }
                Long idOrdemServico = nullableLong(rs, "id_ordem_servico");
                if (idOrdemServico != null) {
                    OrdemServico ordemServico = new OrdemServico();
                    ordemServico.setIdOrdemServico(idOrdemServico);
                    movimento.setOrdemServico(ordemServico);
                }
                movimento.setTipo(rs.getString("tipo"));
                movimento.setDescricao(rs.getString("descricao"));
                movimento.setValor(rs.getBigDecimal("valor"));
                movimento.setDataMovimento(JdbcTemporalValues.getLocalDateTime(rs, "data_movimento"));
                movimento.setFormaPagamento(rs.getString("forma_pagamento"));
                movimento.setCategoria(rs.getString("categoria"));
                return movimento;
            }
        };
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : Long.valueOf(value);
    }

}
