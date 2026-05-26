package com.acaogames.backend.repository;

import com.acaogames.backend.dto.DashboardStatusCount;
import com.acaogames.backend.model.Cliente;
import com.acaogames.backend.model.Equipamento;
import com.acaogames.backend.model.OrdemServico;
import com.acaogames.backend.model.UsuarioSistema;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class OrdemServicoRepository {

    private static final String BASE_SELECT =
            "SELECT os.id_ordem_servico, os.id_cliente, os.id_equipamento, os.id_usuario, os.numero_serie, os.cor, "
                    + "os.data_entrada, os.problema_relatado, os.status, os.diagnostico, os.servico_realizado, "
                    + "os.valor_servico, os.data_saida, os.observacoes, os.data_criacao, os.data_atualizacao, "
                    + "c.id_cliente AS cliente_ref_id, c.nome AS cliente_nome, c.telefone AS cliente_telefone, "
                    + "c.cpf AS cliente_cpf, c.endereco AS cliente_endereco, c.email AS cliente_email, "
                    + "c.observacoes AS cliente_observacoes, c.ativo AS cliente_ativo, c.data_cadastro AS cliente_data_cadastro, "
                    + "e.id_equipamento AS equipamento_ref_id, e.marca AS equipamento_marca, "
                    + "e.tipo_equipamento AS equipamento_tipo, e.modelo AS equipamento_modelo, "
                    + "e.descricao AS equipamento_descricao, e.ativo AS equipamento_ativo, "
                    + "e.data_cadastro AS equipamento_data_cadastro, "
                    + "u.id_usuario AS usuario_ref_id, u.nome AS usuario_nome, u.login AS usuario_login, "
                    + "u.perfil AS usuario_perfil, u.ativo AS usuario_ativo, u.data_cadastro AS usuario_data_cadastro "
                    + "FROM ordem_servico os "
                    + "INNER JOIN cliente c ON c.id_cliente = os.id_cliente "
                    + "INNER JOIN equipamento e ON e.id_equipamento = os.id_equipamento "
                    + "INNER JOIN usuario_sistema u ON u.id_usuario = os.id_usuario ";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OrdemServicoRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<OrdemServico> findAll() {
        return jdbcTemplate.query(BASE_SELECT + "ORDER BY os.id_ordem_servico DESC", rowMapper());
    }

    public Optional<OrdemServico> findById(Long idOrdemServico) {
        List<OrdemServico> ordens = jdbcTemplate.query(
                BASE_SELECT + "WHERE os.id_ordem_servico = :idOrdemServico",
                new MapSqlParameterSource("idOrdemServico", idOrdemServico),
                rowMapper()
        );
        return ordens.stream().findFirst();
    }

    public OrdemServico create(OrdemServico ordemServico) {
        String sql = "INSERT INTO ordem_servico(id_cliente, id_equipamento, id_usuario, numero_serie, cor, "
                + "data_entrada, problema_relatado, status, diagnostico, servico_realizado, valor_servico, "
                + "data_saida, observacoes) VALUES(:idCliente, :idEquipamento, :idUsuario, :numeroSerie, :cor, "
                + ":dataEntrada, :problemaRelatado, :status, :diagnostico, :servicoRealizado, :valorServico, "
                + ":dataSaida, :observacoes)";
        MapSqlParameterSource params = toParams(ordemServico);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id_ordem_servico"});
        Number key = keyHolder.getKey();
        if (key != null) {
            ordemServico.setIdOrdemServico(key.longValue());
        }
        return findById(ordemServico.getIdOrdemServico()).orElse(ordemServico);
    }

    public OrdemServico update(OrdemServico ordemServico) {
        String sql = "UPDATE ordem_servico SET id_cliente = :idCliente, id_equipamento = :idEquipamento, "
                + "id_usuario = :idUsuario, numero_serie = :numeroSerie, cor = :cor, data_entrada = :dataEntrada, "
                + "problema_relatado = :problemaRelatado, status = :status, diagnostico = :diagnostico, "
                + "servico_realizado = :servicoRealizado, valor_servico = :valorServico, data_saida = :dataSaida, "
                + "observacoes = :observacoes, data_atualizacao = CURRENT_TIMESTAMP "
                + "WHERE id_ordem_servico = :idOrdemServico";
        MapSqlParameterSource params = toParams(ordemServico);
        params.addValue("idOrdemServico", ordemServico.getIdOrdemServico());
        jdbcTemplate.update(sql, params);
        return findById(ordemServico.getIdOrdemServico()).orElse(ordemServico);
    }

    public void delete(Long idOrdemServico) {
        jdbcTemplate.update(
                "DELETE FROM ordem_servico WHERE id_ordem_servico = :idOrdemServico",
                new MapSqlParameterSource("idOrdemServico", idOrdemServico)
        );
    }

    public long countByStatuses(List<String> statuses) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ordem_servico WHERE status IN (:statuses)",
                new MapSqlParameterSource("statuses", statuses),
                Long.class
        );
        return total == null ? 0L : total.longValue();
    }

    public BigDecimal sumByStatuses(List<String> statuses) {
        BigDecimal total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(valor_servico), 0) FROM ordem_servico WHERE status IN (:statuses)",
                new MapSqlParameterSource("statuses", statuses),
                BigDecimal.class
        );
        return total == null ? BigDecimal.ZERO : total;
    }

    public BigDecimal sumValorServicoByDataSaida(LocalDate dataSaida) {
        BigDecimal total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(valor_servico), 0) FROM ordem_servico WHERE data_saida = :dataSaida",
                new MapSqlParameterSource("dataSaida", toDate(dataSaida)),
                BigDecimal.class
        );
        return total == null ? BigDecimal.ZERO : total;
    }

    public List<OrdemServico> findRecent(int limit) {
        String sql = BASE_SELECT + "ORDER BY os.data_criacao DESC, os.id_ordem_servico DESC LIMIT :limit";
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("limit", limit),
                rowMapper()
        );
    }

    public List<DashboardStatusCount> countGroupedByStatus() {
        List<DashboardStatusCount> resultado = new ArrayList<DashboardStatusCount>();
        List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) AS total FROM ordem_servico GROUP BY status ORDER BY COUNT(*) DESC, status",
                new MapSqlParameterSource()
        );

        for (java.util.Map<String, Object> row : rows) {
            String status = row.get("status") == null ? null : row.get("status").toString();
            Number total = (Number) row.get("total");
            resultado.add(new DashboardStatusCount(status, total == null ? 0L : total.longValue()));
        }

        return resultado;
    }

    private MapSqlParameterSource toParams(OrdemServico ordemServico) {
        return new MapSqlParameterSource()
                .addValue("idCliente", ordemServico.getCliente().getIdCliente())
                .addValue("idEquipamento", ordemServico.getEquipamento().getIdEquipamento())
                .addValue("idUsuario", ordemServico.getUsuario().getIdUsuario())
                .addValue("numeroSerie", ordemServico.getNumeroSerie())
                .addValue("cor", ordemServico.getCor())
                .addValue("dataEntrada", toDate(ordemServico.getDataEntrada()))
                .addValue("problemaRelatado", ordemServico.getProblemaRelatado())
                .addValue("status", ordemServico.getStatus())
                .addValue("diagnostico", ordemServico.getDiagnostico())
                .addValue("servicoRealizado", ordemServico.getServicoRealizado())
                .addValue("valorServico", ordemServico.getValorServico())
                .addValue("dataSaida", toDate(ordemServico.getDataSaida()))
                .addValue("observacoes", ordemServico.getObservacoes());
    }

    private RowMapper<OrdemServico> rowMapper() {
        return new RowMapper<OrdemServico>() {
            @Override
            public OrdemServico mapRow(ResultSet rs, int rowNum) throws SQLException {
                Cliente cliente = new Cliente();
                cliente.setIdCliente(rs.getLong("cliente_ref_id"));
                cliente.setNome(rs.getString("cliente_nome"));
                cliente.setTelefone(rs.getString("cliente_telefone"));
                cliente.setCpf(rs.getString("cliente_cpf"));
                cliente.setEndereco(rs.getString("cliente_endereco"));
                cliente.setEmail(rs.getString("cliente_email"));
                cliente.setObservacoes(rs.getString("cliente_observacoes"));
                cliente.setAtivo(rs.getBoolean("cliente_ativo"));
                cliente.setDataCadastro(toLocalDateTime(rs.getTimestamp("cliente_data_cadastro")));

                Equipamento equipamento = new Equipamento();
                equipamento.setIdEquipamento(rs.getLong("equipamento_ref_id"));
                equipamento.setMarca(rs.getString("equipamento_marca"));
                equipamento.setTipoEquipamento(rs.getString("equipamento_tipo"));
                equipamento.setModelo(rs.getString("equipamento_modelo"));
                equipamento.setDescricao(rs.getString("equipamento_descricao"));
                equipamento.setAtivo(rs.getBoolean("equipamento_ativo"));
                equipamento.setDataCadastro(toLocalDateTime(rs.getTimestamp("equipamento_data_cadastro")));

                UsuarioSistema usuario = new UsuarioSistema();
                usuario.setIdUsuario(rs.getLong("usuario_ref_id"));
                usuario.setNome(rs.getString("usuario_nome"));
                usuario.setLogin(rs.getString("usuario_login"));
                usuario.setPerfil(rs.getString("usuario_perfil"));
                usuario.setAtivo(rs.getBoolean("usuario_ativo"));
                usuario.setDataCadastro(toLocalDateTime(rs.getTimestamp("usuario_data_cadastro")));

                OrdemServico ordemServico = new OrdemServico();
                ordemServico.setIdOrdemServico(rs.getLong("id_ordem_servico"));
                ordemServico.setCliente(cliente);
                ordemServico.setEquipamento(equipamento);
                ordemServico.setUsuario(usuario);
                ordemServico.setNumeroSerie(rs.getString("numero_serie"));
                ordemServico.setCor(rs.getString("cor"));
                ordemServico.setDataEntrada(toLocalDate(rs.getDate("data_entrada")));
                ordemServico.setProblemaRelatado(rs.getString("problema_relatado"));
                ordemServico.setStatus(rs.getString("status"));
                ordemServico.setDiagnostico(rs.getString("diagnostico"));
                ordemServico.setServicoRealizado(rs.getString("servico_realizado"));
                ordemServico.setValorServico(rs.getBigDecimal("valor_servico"));
                ordemServico.setDataSaida(toLocalDate(rs.getDate("data_saida")));
                ordemServico.setObservacoes(rs.getString("observacoes"));
                ordemServico.setDataCriacao(toLocalDateTime(rs.getTimestamp("data_criacao")));
                ordemServico.setDataAtualizacao(toLocalDateTime(rs.getTimestamp("data_atualizacao")));
                return ordemServico;
            }
        };
    }

    private Date toDate(LocalDate localDate) {
        return localDate == null ? null : Date.valueOf(localDate);
    }

    private LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
