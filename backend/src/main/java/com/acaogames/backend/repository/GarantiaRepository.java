package com.acaogames.backend.repository;

import com.acaogames.backend.model.Garantia;
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
public class GarantiaRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GarantiaRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Garantia> findAll() {
        return jdbcTemplate.query("SELECT * FROM garantia ORDER BY data_inicio DESC, id_garantia DESC", rowMapper());
    }

    public Optional<Garantia> findById(Long idGarantia) {
        List<Garantia> garantias = jdbcTemplate.query(
                "SELECT * FROM garantia WHERE id_garantia = :idGarantia",
                new MapSqlParameterSource("idGarantia", idGarantia),
                rowMapper()
        );
        return garantias.stream().findFirst();
    }

    public Garantia create(Garantia garantia) {
        String sql = "INSERT INTO garantia(id_ordem_servico, data_inicio, data_fim, tempo_garantia_dias, "
                + "descricao, status) VALUES(:idOrdemServico, :dataInicio, :dataFim, :tempoGarantiaDias, "
                + ":descricao, :status)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, toParams(garantia), keyHolder, new String[]{"id_garantia"});
        Number key = keyHolder.getKey();
        if (key != null) {
            garantia.setIdGarantia(key.longValue());
        }
        return findById(garantia.getIdGarantia()).orElse(garantia);
    }

    public Garantia update(Garantia garantia) {
        String sql = "UPDATE garantia SET id_ordem_servico = :idOrdemServico, data_inicio = :dataInicio, "
                + "data_fim = :dataFim, tempo_garantia_dias = :tempoGarantiaDias, descricao = :descricao, "
                + "status = :status WHERE id_garantia = :idGarantia";
        MapSqlParameterSource params = toParams(garantia);
        params.addValue("idGarantia", garantia.getIdGarantia());
        jdbcTemplate.update(sql, params);
        return findById(garantia.getIdGarantia()).orElse(garantia);
    }

    public void delete(Long idGarantia) {
        jdbcTemplate.update(
                "DELETE FROM garantia WHERE id_garantia = :idGarantia",
                new MapSqlParameterSource("idGarantia", idGarantia)
        );
    }

    private MapSqlParameterSource toParams(Garantia garantia) {
        return new MapSqlParameterSource()
                .addValue("idOrdemServico", garantia.getOrdemServico().getIdOrdemServico())
                .addValue("dataInicio", toDate(garantia.getDataInicio()))
                .addValue("dataFim", toDate(garantia.getDataFim()))
                .addValue("tempoGarantiaDias", garantia.getTempoGarantiaDias())
                .addValue("descricao", garantia.getDescricao())
                .addValue("status", garantia.getStatus());
    }

    private RowMapper<Garantia> rowMapper() {
        return new RowMapper<Garantia>() {
            @Override
            public Garantia mapRow(ResultSet rs, int rowNum) throws SQLException {
                OrdemServico ordemServico = new OrdemServico();
                ordemServico.setIdOrdemServico(rs.getLong("id_ordem_servico"));

                Garantia garantia = new Garantia();
                garantia.setIdGarantia(rs.getLong("id_garantia"));
                garantia.setOrdemServico(ordemServico);
                garantia.setDataInicio(toLocalDate(rs.getDate("data_inicio")));
                garantia.setDataFim(toLocalDate(rs.getDate("data_fim")));
                garantia.setTempoGarantiaDias(rs.getInt("tempo_garantia_dias"));
                garantia.setDescricao(rs.getString("descricao"));
                garantia.setStatus(rs.getString("status"));
                return garantia;
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
