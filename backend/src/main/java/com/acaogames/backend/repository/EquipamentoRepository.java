package com.acaogames.backend.repository;

import com.acaogames.backend.model.Equipamento;
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
public class EquipamentoRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public EquipamentoRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Equipamento> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM equipamento ORDER BY marca, tipo_equipamento, modelo",
                rowMapper()
        );
    }

    public Optional<Equipamento> findById(Long idEquipamento) {
        List<Equipamento> equipamentos = jdbcTemplate.query(
                "SELECT * FROM equipamento WHERE id_equipamento = :idEquipamento",
                new MapSqlParameterSource("idEquipamento", idEquipamento),
                rowMapper()
        );
        return equipamentos.stream().findFirst();
    }

    public Equipamento create(Equipamento equipamento) {
        String sql = "INSERT INTO equipamento(marca, tipo_equipamento, modelo, descricao, ativo) "
                + "VALUES(:marca, :tipoEquipamento, :modelo, :descricao, :ativo)";
        MapSqlParameterSource params = toParams(equipamento);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id_equipamento"});
        Number key = keyHolder.getKey();
        if (key != null) {
            equipamento.setIdEquipamento(key.longValue());
        }
        return findById(equipamento.getIdEquipamento()).orElse(equipamento);
    }

    public Equipamento update(Equipamento equipamento) {
        String sql = "UPDATE equipamento SET marca = :marca, tipo_equipamento = :tipoEquipamento, "
                + "modelo = :modelo, descricao = :descricao, ativo = :ativo WHERE id_equipamento = :idEquipamento";
        MapSqlParameterSource params = toParams(equipamento);
        params.addValue("idEquipamento", equipamento.getIdEquipamento());
        jdbcTemplate.update(sql, params);
        return findById(equipamento.getIdEquipamento()).orElse(equipamento);
    }

    public void delete(Long idEquipamento) {
        jdbcTemplate.update(
                "DELETE FROM equipamento WHERE id_equipamento = :idEquipamento",
                new MapSqlParameterSource("idEquipamento", idEquipamento)
        );
    }

    private MapSqlParameterSource toParams(Equipamento equipamento) {
        return new MapSqlParameterSource()
                .addValue("marca", equipamento.getMarca())
                .addValue("tipoEquipamento", equipamento.getTipoEquipamento())
                .addValue("modelo", equipamento.getModelo())
                .addValue("descricao", equipamento.getDescricao())
                .addValue("ativo", equipamento.isAtivo());
    }

    private RowMapper<Equipamento> rowMapper() {
        return new RowMapper<Equipamento>() {
            @Override
            public Equipamento mapRow(ResultSet rs, int rowNum) throws SQLException {
                Equipamento equipamento = new Equipamento();
                equipamento.setIdEquipamento(rs.getLong("id_equipamento"));
                equipamento.setMarca(rs.getString("marca"));
                equipamento.setTipoEquipamento(rs.getString("tipo_equipamento"));
                equipamento.setModelo(rs.getString("modelo"));
                equipamento.setDescricao(rs.getString("descricao"));
                equipamento.setAtivo(rs.getBoolean("ativo"));
                equipamento.setDataCadastro(toLocalDateTime(rs.getTimestamp("data_cadastro")));
                return equipamento;
            }
        };
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
