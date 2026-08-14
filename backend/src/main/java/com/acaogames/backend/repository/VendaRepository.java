package com.acaogames.backend.repository;

import com.acaogames.backend.model.Cliente;
import com.acaogames.backend.model.Venda;
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
public class VendaRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public VendaRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Venda> findAll() {
        return jdbcTemplate.query("SELECT * FROM venda ORDER BY data_venda DESC, id_venda DESC", rowMapper());
    }

    public List<Venda> findRecent(int limit) {
        return jdbcTemplate.query(
                "SELECT * FROM venda ORDER BY data_venda DESC, id_venda DESC LIMIT :limit",
                new MapSqlParameterSource("limit", limit),
                rowMapper()
        );
    }

    public Optional<Venda> findById(Long idVenda) {
        List<Venda> vendas = jdbcTemplate.query(
                "SELECT * FROM venda WHERE id_venda = :idVenda",
                new MapSqlParameterSource("idVenda", idVenda),
                rowMapper()
        );
        return vendas.stream().findFirst();
    }

    public Venda create(Venda venda) {
        String sql = "INSERT INTO venda(id_cliente, data_venda, valor_total, forma_pagamento, garantia_dias, observacoes) "
                + "VALUES(:idCliente, :dataVenda, :valorTotal, :formaPagamento, :garantiaDias, :observacoes)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, toParams(venda), keyHolder, new String[]{"id_venda"});
        Number key = keyHolder.getKey();
        if (key != null) {
            venda.setIdVenda(key.longValue());
        }
        return findById(venda.getIdVenda()).orElse(venda);
    }

    public Venda update(Venda venda) {
        String sql = "UPDATE venda SET id_cliente = :idCliente, data_venda = :dataVenda, "
                + "valor_total = :valorTotal, forma_pagamento = :formaPagamento, garantia_dias = :garantiaDias, "
                + "observacoes = :observacoes "
                + "WHERE id_venda = :idVenda";
        MapSqlParameterSource params = toParams(venda);
        params.addValue("idVenda", venda.getIdVenda());
        jdbcTemplate.update(sql, params);
        return findById(venda.getIdVenda()).orElse(venda);
    }

    public void delete(Long idVenda) {
        jdbcTemplate.update(
                "DELETE FROM venda WHERE id_venda = :idVenda",
                new MapSqlParameterSource("idVenda", idVenda)
        );
    }

    private MapSqlParameterSource toParams(Venda venda) {
        Long idCliente = venda.getCliente() == null ? null : venda.getCliente().getIdCliente();
        return new MapSqlParameterSource()
                .addValue("idCliente", idCliente)
                .addValue("dataVenda", JdbcTemporalValues.toDatabaseValue(venda.getDataVenda()))
                .addValue("valorTotal", venda.getValorTotal())
                .addValue("formaPagamento", venda.getFormaPagamento())
                .addValue("garantiaDias", venda.getGarantiaDias() == null ? 0 : venda.getGarantiaDias())
                .addValue("observacoes", venda.getObservacoes());
    }

    private RowMapper<Venda> rowMapper() {
        return new RowMapper<Venda>() {
            @Override
            public Venda mapRow(ResultSet rs, int rowNum) throws SQLException {
                Venda venda = new Venda();
                venda.setIdVenda(rs.getLong("id_venda"));
                Long idCliente = nullableLong(rs, "id_cliente");
                if (idCliente != null) {
                    Cliente cliente = new Cliente();
                    cliente.setIdCliente(idCliente);
                    venda.setCliente(cliente);
                }
                venda.setDataVenda(JdbcTemporalValues.getLocalDate(rs, "data_venda"));
                venda.setValorTotal(rs.getBigDecimal("valor_total"));
                venda.setFormaPagamento(rs.getString("forma_pagamento"));
                venda.setGarantiaDias(rs.getInt("garantia_dias"));
                venda.setObservacoes(rs.getString("observacoes"));
                return venda;
            }
        };
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : Long.valueOf(value);
    }

}
