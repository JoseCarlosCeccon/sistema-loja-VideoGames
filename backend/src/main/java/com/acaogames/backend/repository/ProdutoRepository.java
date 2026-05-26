package com.acaogames.backend.repository;

import com.acaogames.backend.model.Produto;
import com.acaogames.backend.model.ProdutoFoto;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ProdutoRepository {

    private static final String BASE_SELECT = "SELECT id_produto, nome, categoria, descricao, quantidade_estoque, "
            + "estoque_minimo, valor_compra, valor_venda, ativo, data_cadastro, "
            + "(foto IS NOT NULL) AS possui_foto, foto_nome, foto_tipo, foto_tamanho FROM produto ";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ProdutoRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Produto> findAll() {
        return jdbcTemplate.query(BASE_SELECT + "ORDER BY nome", rowMapper());
    }

    public Optional<Produto> findById(Long idProduto) {
        List<Produto> produtos = jdbcTemplate.query(
                BASE_SELECT + "WHERE id_produto = :idProduto",
                new MapSqlParameterSource("idProduto", idProduto),
                rowMapper()
        );
        return produtos.stream().findFirst();
    }

    public Produto create(Produto produto) {
        String sql = "INSERT INTO produto(nome, categoria, descricao, quantidade_estoque, estoque_minimo, "
                + "valor_compra, valor_venda, ativo) VALUES(:nome, :categoria, :descricao, :quantidadeEstoque, "
                + ":estoqueMinimo, :valorCompra, :valorVenda, :ativo)";
        MapSqlParameterSource params = toParams(produto);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id_produto"});
        Number key = keyHolder.getKey();
        if (key != null) {
            produto.setIdProduto(key.longValue());
        }
        return findById(produto.getIdProduto()).orElse(produto);
    }

    public Produto update(Produto produto) {
        String sql = "UPDATE produto SET nome = :nome, categoria = :categoria, descricao = :descricao, "
                + "quantidade_estoque = :quantidadeEstoque, estoque_minimo = :estoqueMinimo, "
                + "valor_compra = :valorCompra, valor_venda = :valorVenda, ativo = :ativo "
                + "WHERE id_produto = :idProduto";
        MapSqlParameterSource params = toParams(produto);
        params.addValue("idProduto", produto.getIdProduto());
        jdbcTemplate.update(sql, params);
        return findById(produto.getIdProduto()).orElse(produto);
    }

    public void delete(Long idProduto) {
        jdbcTemplate.update(
                "DELETE FROM produto WHERE id_produto = :idProduto",
                new MapSqlParameterSource("idProduto", idProduto)
        );
    }

    public void updateFoto(Long idProduto, byte[] foto, String nome, String tipo, int tamanho) {
        jdbcTemplate.update(
                "UPDATE produto SET foto = :foto, foto_nome = :fotoNome, foto_tipo = :fotoTipo, "
                        + "foto_tamanho = :fotoTamanho WHERE id_produto = :idProduto",
                new MapSqlParameterSource()
                        .addValue("idProduto", idProduto)
                        .addValue("foto", foto)
                        .addValue("fotoNome", nome)
                        .addValue("fotoTipo", tipo)
                        .addValue("fotoTamanho", tamanho)
        );
    }

    public void deleteFoto(Long idProduto) {
        jdbcTemplate.update(
                "UPDATE produto SET foto = NULL, foto_nome = NULL, foto_tipo = NULL, foto_tamanho = NULL "
                        + "WHERE id_produto = :idProduto",
                new MapSqlParameterSource("idProduto", idProduto)
        );
    }

    public Optional<ProdutoFoto> findFotoById(Long idProduto) {
        List<ProdutoFoto> fotos = jdbcTemplate.query(
                "SELECT foto, foto_nome, foto_tipo, foto_tamanho FROM produto WHERE id_produto = :idProduto "
                        + "AND foto IS NOT NULL",
                new MapSqlParameterSource("idProduto", idProduto),
                new RowMapper<ProdutoFoto>() {
                    @Override
                    public ProdutoFoto mapRow(ResultSet rs, int rowNum) throws SQLException {
                        ProdutoFoto foto = new ProdutoFoto();
                        byte[] conteudo = rs.getBytes("foto");
                        foto.setConteudo(conteudo);
                        foto.setNome(rs.getString("foto_nome"));
                        foto.setTipo(rs.getString("foto_tipo"));
                        foto.setTamanho(rs.getInt("foto_tamanho"));
                        if (rs.wasNull() && conteudo != null) {
                            foto.setTamanho(conteudo.length);
                        }
                        return foto;
                    }
                }
        );
        return fotos.stream().findFirst();
    }

    public long countAll() {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM produto",
                new MapSqlParameterSource(),
                Long.class
        );
        return total == null ? 0L : total.longValue();
    }

    public long countLowStock() {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM produto WHERE quantidade_estoque <= estoque_minimo",
                new MapSqlParameterSource(),
                Long.class
        );
        return total == null ? 0L : total.longValue();
    }

    public boolean debitarEstoque(Long idProduto, int quantidade) {
        int updated = jdbcTemplate.update(
                "UPDATE produto SET quantidade_estoque = quantidade_estoque - :quantidade "
                        + "WHERE id_produto = :idProduto AND quantidade_estoque >= :quantidade",
                new MapSqlParameterSource()
                        .addValue("idProduto", idProduto)
                        .addValue("quantidade", quantidade)
        );
        return updated > 0;
    }

    public void creditarEstoque(Long idProduto, int quantidade) {
        jdbcTemplate.update(
                "UPDATE produto SET quantidade_estoque = quantidade_estoque + :quantidade "
                        + "WHERE id_produto = :idProduto",
                new MapSqlParameterSource()
                        .addValue("idProduto", idProduto)
                        .addValue("quantidade", quantidade)
        );
    }

    private MapSqlParameterSource toParams(Produto produto) {
        BigDecimal valorCompra = produto.getValorCompra() == null ? BigDecimal.ZERO : produto.getValorCompra();
        BigDecimal valorVenda = produto.getValorVenda() == null ? BigDecimal.ZERO : produto.getValorVenda();

        return new MapSqlParameterSource()
                .addValue("nome", produto.getNome())
                .addValue("categoria", produto.getCategoria())
                .addValue("descricao", produto.getDescricao())
                .addValue("quantidadeEstoque", produto.getQuantidadeEstoque())
                .addValue("estoqueMinimo", produto.getEstoqueMinimo())
                .addValue("valorCompra", valorCompra)
                .addValue("valorVenda", valorVenda)
                .addValue("ativo", produto.isAtivo());
    }

    private RowMapper<Produto> rowMapper() {
        return new RowMapper<Produto>() {
            @Override
            public Produto mapRow(ResultSet rs, int rowNum) throws SQLException {
                Produto produto = new Produto();
                produto.setIdProduto(rs.getLong("id_produto"));
                produto.setNome(rs.getString("nome"));
                produto.setCategoria(rs.getString("categoria"));
                produto.setDescricao(rs.getString("descricao"));
                produto.setQuantidadeEstoque(rs.getInt("quantidade_estoque"));
                produto.setEstoqueMinimo(rs.getInt("estoque_minimo"));
                produto.setValorCompra(rs.getBigDecimal("valor_compra"));
                produto.setValorVenda(rs.getBigDecimal("valor_venda"));
                produto.setPossuiFoto(rs.getBoolean("possui_foto"));
                produto.setFotoNome(rs.getString("foto_nome"));
                produto.setFotoTipo(rs.getString("foto_tipo"));
                int fotoTamanho = rs.getInt("foto_tamanho");
                produto.setFotoTamanho(rs.wasNull() ? null : Integer.valueOf(fotoTamanho));
                produto.setAtivo(rs.getBoolean("ativo"));
                produto.setDataCadastro(toLocalDateTime(rs.getTimestamp("data_cadastro")));
                return produto;
            }
        };
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
