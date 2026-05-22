package com.acaogames.backend.repository;

import com.acaogames.backend.model.ItemVenda;
import com.acaogames.backend.model.Produto;
import com.acaogames.backend.model.Venda;
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
public class ItemVendaRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ItemVendaRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ItemVenda> findAll() {
        return jdbcTemplate.query("SELECT * FROM item_venda ORDER BY id_item_venda DESC", rowMapper());
    }

    public List<ItemVenda> findByVendaId(Long idVenda) {
        return jdbcTemplate.query(
                "SELECT * FROM item_venda WHERE id_venda = :idVenda ORDER BY id_item_venda",
                new MapSqlParameterSource("idVenda", idVenda),
                rowMapper()
        );
    }

    public Optional<ItemVenda> findById(Long idItemVenda) {
        List<ItemVenda> itens = jdbcTemplate.query(
                "SELECT * FROM item_venda WHERE id_item_venda = :idItemVenda",
                new MapSqlParameterSource("idItemVenda", idItemVenda),
                rowMapper()
        );
        return itens.stream().findFirst();
    }

    public ItemVenda create(ItemVenda item) {
        String sql = "INSERT INTO item_venda(id_venda, id_produto, quantidade, valor_unitario, subtotal) "
                + "VALUES(:idVenda, :idProduto, :quantidade, :valorUnitario, :subtotal)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, toParams(item), keyHolder, new String[]{"id_item_venda"});
        Number key = keyHolder.getKey();
        if (key != null) {
            item.setIdItemVenda(key.longValue());
        }
        return findById(item.getIdItemVenda()).orElse(item);
    }

    public void deleteByVendaId(Long idVenda) {
        jdbcTemplate.update(
                "DELETE FROM item_venda WHERE id_venda = :idVenda",
                new MapSqlParameterSource("idVenda", idVenda)
        );
    }

    public void delete(Long idItemVenda) {
        jdbcTemplate.update(
                "DELETE FROM item_venda WHERE id_item_venda = :idItemVenda",
                new MapSqlParameterSource("idItemVenda", idItemVenda)
        );
    }

    private MapSqlParameterSource toParams(ItemVenda item) {
        return new MapSqlParameterSource()
                .addValue("idVenda", item.getVenda().getIdVenda())
                .addValue("idProduto", item.getProduto().getIdProduto())
                .addValue("quantidade", item.getQuantidade())
                .addValue("valorUnitario", item.getValorUnitario())
                .addValue("subtotal", item.getSubtotal());
    }

    private RowMapper<ItemVenda> rowMapper() {
        return new RowMapper<ItemVenda>() {
            @Override
            public ItemVenda mapRow(ResultSet rs, int rowNum) throws SQLException {
                Venda venda = new Venda();
                venda.setIdVenda(rs.getLong("id_venda"));

                Produto produto = new Produto();
                produto.setIdProduto(rs.getLong("id_produto"));

                ItemVenda item = new ItemVenda();
                item.setIdItemVenda(rs.getLong("id_item_venda"));
                item.setVenda(venda);
                item.setProduto(produto);
                item.setQuantidade(rs.getInt("quantidade"));
                item.setValorUnitario(rs.getBigDecimal("valor_unitario"));
                item.setSubtotal(rs.getBigDecimal("subtotal"));
                return item;
            }
        };
    }
}
