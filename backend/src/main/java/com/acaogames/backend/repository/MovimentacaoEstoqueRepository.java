package com.acaogames.backend.repository;

import com.acaogames.backend.model.MovimentacaoEstoque;
import com.acaogames.backend.model.Produto;
import com.acaogames.backend.model.UsuarioSistema;
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
public class MovimentacaoEstoqueRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MovimentacaoEstoqueRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MovimentacaoEstoque> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM movimentacao_estoque ORDER BY data_movimentacao DESC, id_movimentacao DESC",
                rowMapper()
        );
    }

    public Optional<MovimentacaoEstoque> findById(Long idMovimentacao) {
        List<MovimentacaoEstoque> movimentacoes = jdbcTemplate.query(
                "SELECT * FROM movimentacao_estoque WHERE id_movimentacao = :idMovimentacao",
                new MapSqlParameterSource("idMovimentacao", idMovimentacao),
                rowMapper()
        );
        return movimentacoes.stream().findFirst();
    }

    public MovimentacaoEstoque create(MovimentacaoEstoque movimentacao) {
        String sql = "INSERT INTO movimentacao_estoque(id_produto, id_usuario, tipo, quantidade, "
                + "data_movimentacao, motivo, observacoes) VALUES(:idProduto, :idUsuario, :tipo, "
                + ":quantidade, :dataMovimentacao, :motivo, :observacoes)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, toParams(movimentacao), keyHolder, new String[]{"id_movimentacao"});
        Number key = keyHolder.getKey();
        if (key != null) {
            movimentacao.setIdMovimentacao(key.longValue());
        }
        return findById(movimentacao.getIdMovimentacao()).orElse(movimentacao);
    }

    public void delete(Long idMovimentacao) {
        jdbcTemplate.update(
                "DELETE FROM movimentacao_estoque WHERE id_movimentacao = :idMovimentacao",
                new MapSqlParameterSource("idMovimentacao", idMovimentacao)
        );
    }

    private MapSqlParameterSource toParams(MovimentacaoEstoque movimentacao) {
        return new MapSqlParameterSource()
                .addValue("idProduto", movimentacao.getProduto().getIdProduto())
                .addValue("idUsuario", movimentacao.getUsuario().getIdUsuario())
                .addValue("tipo", movimentacao.getTipo())
                .addValue("quantidade", movimentacao.getQuantidade())
                .addValue("dataMovimentacao", toTimestamp(movimentacao.getDataMovimentacao()))
                .addValue("motivo", movimentacao.getMotivo())
                .addValue("observacoes", movimentacao.getObservacoes());
    }

    private RowMapper<MovimentacaoEstoque> rowMapper() {
        return new RowMapper<MovimentacaoEstoque>() {
            @Override
            public MovimentacaoEstoque mapRow(ResultSet rs, int rowNum) throws SQLException {
                Produto produto = new Produto();
                produto.setIdProduto(rs.getLong("id_produto"));

                UsuarioSistema usuario = new UsuarioSistema();
                usuario.setIdUsuario(rs.getLong("id_usuario"));

                MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
                movimentacao.setIdMovimentacao(rs.getLong("id_movimentacao"));
                movimentacao.setProduto(produto);
                movimentacao.setUsuario(usuario);
                movimentacao.setTipo(rs.getString("tipo"));
                movimentacao.setQuantidade(rs.getInt("quantidade"));
                movimentacao.setDataMovimentacao(toLocalDateTime(rs.getTimestamp("data_movimentacao")));
                movimentacao.setMotivo(rs.getString("motivo"));
                movimentacao.setObservacoes(rs.getString("observacoes"));
                return movimentacao;
            }
        };
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
