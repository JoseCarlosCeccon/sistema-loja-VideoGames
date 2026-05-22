package com.acaogames.backend.repository;

import com.acaogames.backend.model.ItemServico;
import com.acaogames.backend.model.OrdemServico;
import com.acaogames.backend.model.Produto;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ItemServicoRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ItemServicoRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ItemServico> findAll() {
        return jdbcTemplate.query("SELECT * FROM item_servico ORDER BY id_item_servico DESC", rowMapper());
    }

    public List<ItemServico> findByOrdemServicoId(Long idOrdemServico) {
        return jdbcTemplate.query(
                "SELECT * FROM item_servico WHERE id_ordem_servico = :idOrdemServico ORDER BY id_item_servico",
                new MapSqlParameterSource("idOrdemServico", idOrdemServico),
                rowMapper()
        );
    }

    public Optional<ItemServico> findById(Long idItemServico) {
        List<ItemServico> itens = jdbcTemplate.query(
                "SELECT * FROM item_servico WHERE id_item_servico = :idItemServico",
                new MapSqlParameterSource("idItemServico", idItemServico),
                rowMapper()
        );
        return itens.stream().findFirst();
    }

    public ItemServico create(ItemServico item) {
        String sql = "INSERT INTO item_servico(id_ordem_servico, id_produto, quantidade, valor_unitario, subtotal) "
                + "VALUES(:idOrdemServico, :idProduto, :quantidade, :valorUnitario, :subtotal)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, toParams(item), keyHolder, new String[]{"id_item_servico"});
        Number key = keyHolder.getKey();
        if (key != null) {
            item.setIdItemServico(key.longValue());
        }
        return findById(item.getIdItemServico()).orElse(item);
    }

    public ItemServico update(ItemServico item) {
        String sql = "UPDATE item_servico SET id_ordem_servico = :idOrdemServico, id_produto = :idProduto, "
                + "quantidade = :quantidade, valor_unitario = :valorUnitario, subtotal = :subtotal "
                + "WHERE id_item_servico = :idItemServico";
        MapSqlParameterSource params = toParams(item);
        params.addValue("idItemServico", item.getIdItemServico());
        jdbcTemplate.update(sql, params);
        return findById(item.getIdItemServico()).orElse(item);
    }

    public void delete(Long idItemServico) {
        jdbcTemplate.update(
                "DELETE FROM item_servico WHERE id_item_servico = :idItemServico",
                new MapSqlParameterSource("idItemServico", idItemServico)
        );
    }

    private MapSqlParameterSource toParams(ItemServico item) {
        return new MapSqlParameterSource()
                .addValue("idOrdemServico", item.getOrdemServico().getIdOrdemServico())
                .addValue("idProduto", item.getProduto().getIdProduto())
                .addValue("quantidade", item.getQuantidade())
                .addValue("valorUnitario", item.getValorUnitario())
                .addValue("subtotal", item.getSubtotal());
    }

    private RowMapper<ItemServico> rowMapper() {
        return new RowMapper<ItemServico>() {
            @Override
            public ItemServico mapRow(ResultSet rs, int rowNum) throws SQLException {
                OrdemServico ordemServico = new OrdemServico();
                ordemServico.setIdOrdemServico(rs.getLong("id_ordem_servico"));

                Produto produto = new Produto();
                produto.setIdProduto(rs.getLong("id_produto"));

                ItemServico item = new ItemServico();
                item.setIdItemServico(rs.getLong("id_item_servico"));
                item.setOrdemServico(ordemServico);
                item.setProduto(produto);
                item.setQuantidade(rs.getInt("quantidade"));
                item.setValorUnitario(rs.getBigDecimal("valor_unitario"));
                item.setSubtotal(rs.getBigDecimal("subtotal"));
                return item;
            }
        };
    }
}
