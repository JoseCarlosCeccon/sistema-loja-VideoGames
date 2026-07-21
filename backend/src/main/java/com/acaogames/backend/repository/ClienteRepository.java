package com.acaogames.backend.repository;

import com.acaogames.backend.model.Cliente;
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
public class ClienteRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ClienteRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Cliente> findAll() {
        return jdbcTemplate.query("SELECT * FROM cliente ORDER BY nome", rowMapper());
    }

    public Optional<Cliente> findById(Long idCliente) {
        List<Cliente> clientes = jdbcTemplate.query(
                "SELECT * FROM cliente WHERE id_cliente = :idCliente",
                new MapSqlParameterSource("idCliente", idCliente),
                rowMapper()
        );
        return clientes.stream().findFirst();
    }

    public Cliente create(Cliente cliente) {
        String sql = "INSERT INTO cliente(nome, telefone, cpf, endereco, email, observacoes, ativo) "
                + "VALUES(:nome, :telefone, :cpf, :endereco, :email, :observacoes, :ativo)";
        MapSqlParameterSource params = toParams(cliente);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id_cliente"});
        Number key = keyHolder.getKey();
        if (key != null) {
            cliente.setIdCliente(key.longValue());
        }
        return findById(cliente.getIdCliente()).orElse(cliente);
    }

    public Cliente update(Cliente cliente) {
        String sql = "UPDATE cliente SET nome = :nome, telefone = :telefone, cpf = :cpf, endereco = :endereco, "
                + "email = :email, observacoes = :observacoes, ativo = :ativo WHERE id_cliente = :idCliente";
        MapSqlParameterSource params = toParams(cliente);
        params.addValue("idCliente", cliente.getIdCliente());
        jdbcTemplate.update(sql, params);
        return findById(cliente.getIdCliente()).orElse(cliente);
    }

    public void delete(Long idCliente) {
        jdbcTemplate.update(
                "DELETE FROM cliente WHERE id_cliente = :idCliente",
                new MapSqlParameterSource("idCliente", idCliente)
        );
    }

    public long countActive() {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cliente WHERE ativo = TRUE",
                new MapSqlParameterSource(),
                Long.class
        );
        return total == null ? 0L : total.longValue();
    }

    private MapSqlParameterSource toParams(Cliente cliente) {
        return new MapSqlParameterSource()
                .addValue("nome", cliente.getNome())
                .addValue("telefone", cliente.getTelefone())
                .addValue("cpf", cliente.getCpf())
                .addValue("endereco", cliente.getEndereco())
                .addValue("email", cliente.getEmail())
                .addValue("observacoes", cliente.getObservacoes())
                .addValue("ativo", cliente.isAtivo());
    }

    private RowMapper<Cliente> rowMapper() {
        return new RowMapper<Cliente>() {
            @Override
            public Cliente mapRow(ResultSet rs, int rowNum) throws SQLException {
                Cliente cliente = new Cliente();
                cliente.setIdCliente(rs.getLong("id_cliente"));
                cliente.setNome(rs.getString("nome"));
                cliente.setTelefone(rs.getString("telefone"));
                cliente.setCpf(rs.getString("cpf"));
                cliente.setEndereco(rs.getString("endereco"));
                cliente.setEmail(rs.getString("email"));
                cliente.setObservacoes(rs.getString("observacoes"));
                cliente.setAtivo(rs.getBoolean("ativo"));
                cliente.setDataCadastro(JdbcTemporalValues.getLocalDateTime(rs, "data_cadastro"));
                return cliente;
            }
        };
    }

}
