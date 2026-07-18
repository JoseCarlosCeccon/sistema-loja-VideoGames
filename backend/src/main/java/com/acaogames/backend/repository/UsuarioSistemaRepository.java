package com.acaogames.backend.repository;

import com.acaogames.backend.model.UsuarioSistema;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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

    public Optional<UsuarioSistema> findByLogin(String login) {
        List<UsuarioSistema> usuarios = jdbcTemplate.query(
                "SELECT * FROM usuario_sistema WHERE login = :login",
                new MapSqlParameterSource("login", login),
                rowMapper()
        );
        return usuarios.stream().findFirst();
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
                usuario.setDataCadastro(JdbcTemporalValues.getLocalDateTime(rs, "data_cadastro"));
                return usuario;
            }
        };
    }

}
