package com.acaogames.backend.repository;

import com.acaogames.backend.model.Orcamento;
import com.acaogames.backend.model.OrdemServico;
import java.sql.Date;
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
public class OrcamentoRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OrcamentoRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Orcamento> findAll() {
        return jdbcTemplate.query("SELECT * FROM orcamento ORDER BY data_orcamento DESC, id_orcamento DESC", rowMapper());
    }

    public Optional<Orcamento> findById(Long idOrcamento) {
        List<Orcamento> itens = jdbcTemplate.query(
                "SELECT * FROM orcamento WHERE id_orcamento = :idOrcamento",
                new MapSqlParameterSource("idOrcamento", idOrcamento),
                rowMapper()
        );
        return itens.stream().findFirst();
    }

    public Optional<Orcamento> findByOrdemServicoId(Long idOrdemServico) {
        List<Orcamento> itens = jdbcTemplate.query(
                "SELECT * FROM orcamento WHERE id_ordem_servico = :idOrdemServico",
                new MapSqlParameterSource("idOrdemServico", idOrdemServico),
                rowMapper()
        );
        return itens.stream().findFirst();
    }

    public Orcamento create(Orcamento orcamento) {
        String sql = "INSERT INTO orcamento(id_ordem_servico, descricao, valor_estimado, data_orcamento, "
                + "status_orcamento, observacoes) VALUES(:idOrdemServico, :descricao, :valorEstimado, "
                + ":dataOrcamento, :statusOrcamento, :observacoes)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, toParams(orcamento), keyHolder, new String[]{"id_orcamento"});
        Number key = keyHolder.getKey();
        if (key != null) {
            orcamento.setIdOrcamento(key.longValue());
        }
        return findById(orcamento.getIdOrcamento()).orElse(orcamento);
    }

    public Orcamento update(Orcamento orcamento) {
        String sql = "UPDATE orcamento SET id_ordem_servico = :idOrdemServico, descricao = :descricao, "
                + "valor_estimado = :valorEstimado, data_orcamento = :dataOrcamento, "
                + "status_orcamento = :statusOrcamento, observacoes = :observacoes "
                + "WHERE id_orcamento = :idOrcamento";
        MapSqlParameterSource params = toParams(orcamento);
        params.addValue("idOrcamento", orcamento.getIdOrcamento());
        jdbcTemplate.update(sql, params);
        return findById(orcamento.getIdOrcamento()).orElse(orcamento);
    }

    public void delete(Long idOrcamento) {
        jdbcTemplate.update(
                "DELETE FROM orcamento WHERE id_orcamento = :idOrcamento",
                new MapSqlParameterSource("idOrcamento", idOrcamento)
        );
    }

    private MapSqlParameterSource toParams(Orcamento orcamento) {
        return new MapSqlParameterSource()
                .addValue("idOrdemServico", orcamento.getOrdemServico().getIdOrdemServico())
                .addValue("descricao", orcamento.getDescricao())
                .addValue("valorEstimado", orcamento.getValorEstimado())
                .addValue("dataOrcamento", toDate(orcamento.getDataOrcamento()))
                .addValue("statusOrcamento", orcamento.getStatusOrcamento())
                .addValue("observacoes", orcamento.getObservacoes());
    }

    private RowMapper<Orcamento> rowMapper() {
        return new RowMapper<Orcamento>() {
            @Override
            public Orcamento mapRow(ResultSet rs, int rowNum) throws SQLException {
                OrdemServico ordemServico = new OrdemServico();
                ordemServico.setIdOrdemServico(rs.getLong("id_ordem_servico"));

                Orcamento orcamento = new Orcamento();
                orcamento.setIdOrcamento(rs.getLong("id_orcamento"));
                orcamento.setOrdemServico(ordemServico);
                orcamento.setDescricao(rs.getString("descricao"));
                orcamento.setValorEstimado(rs.getBigDecimal("valor_estimado"));
                orcamento.setDataOrcamento(toLocalDate(rs.getDate("data_orcamento")));
                orcamento.setStatusOrcamento(rs.getString("status_orcamento"));
                orcamento.setObservacoes(rs.getString("observacoes"));
                return orcamento;
            }
        };
    }

    private Date toDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    private LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }
}
