package com.acaogames.backend.repository;

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
public class UsuarioSistemaRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UsuarioSistemaRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UsuarioSistema> findById(Long idUsuario) {
        List<UsuarioSistema> usuarios = jdbcTemplate.query(
                "SELECT * FROM usuario_sistema WHERE id_usuario = :idUsuario",
                new MapSqlParameterSource("idUsuario", idUsuario),
                rowMapper()
        );
        return usuarios.stream().findFirst();
    }

    public List<UsuarioSistema> findAll() {
        return jdbcTemplate.query("SELECT * FROM usuario_sistema ORDER BY nome", rowMapper());
    }

    public Optional<UsuarioSistema> findByLogin(String login) {
        List<UsuarioSistema> usuarios = jdbcTemplate.query(
                "SELECT * FROM usuario_sistema WHERE login = :login",
                new MapSqlParameterSource("login", login),
                rowMapper()
        );
        return usuarios.stream().findFirst();
    }

    public UsuarioSistema create(UsuarioSistema usuario) {
        String sql = "INSERT INTO usuario_sistema(nome, login, senha, perfil, ativo) "
                + "VALUES(:nome, :login, :senha, :perfil, :ativo)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, toParams(usuario), keyHolder, new String[]{"id_usuario"});
        Number key = keyHolder.getKey();
        if (key != null) {
            usuario.setIdUsuario(key.longValue());
        }
        return findById(usuario.getIdUsuario()).orElse(usuario);
    }

    public UsuarioSistema update(UsuarioSistema usuario) {
        String sql = "UPDATE usuario_sistema SET nome = :nome, login = :login, senha = :senha, "
                + "perfil = :perfil, ativo = :ativo WHERE id_usuario = :idUsuario";
        MapSqlParameterSource params = toParams(usuario);
        params.addValue("idUsuario", usuario.getIdUsuario());
        jdbcTemplate.update(sql, params);
        return findById(usuario.getIdUsuario()).orElse(usuario);
    }

    public void delete(Long idUsuario) {
        jdbcTemplate.update(
                "DELETE FROM usuario_sistema WHERE id_usuario = :idUsuario",
                new MapSqlParameterSource("idUsuario", idUsuario)
        );
    }

    private MapSqlParameterSource toParams(UsuarioSistema usuario) {
        return new MapSqlParameterSource()
                .addValue("nome", usuario.getNome())
                .addValue("login", usuario.getLogin())
                .addValue("senha", usuario.getSenha())
                .addValue("perfil", usuario.getPerfil())
                .addValue("ativo", usuario.isAtivo());
    }

    private RowMapper<UsuarioSistema> rowMapper() {
        return new RowMapper<UsuarioSistema>() {
            @Override
            public UsuarioSistema mapRow(ResultSet rs, int rowNum) throws SQLException {
                UsuarioSistema usuario = new UsuarioSistema();
                usuario.setIdUsuario(rs.getLong("id_usuario"));
                usuario.setNome(rs.getString("nome"));
                usuario.setLogin(rs.getString("login"));
                usuario.setSenha(rs.getString("senha"));
                usuario.setPerfil(rs.getString("perfil"));
                usuario.setAtivo(rs.getBoolean("ativo"));
                usuario.setDataCadastro(toLocalDateTime(rs.getTimestamp("data_cadastro")));
                return usuario;
            }
        };
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
