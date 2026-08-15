package com.acaogames.backend.config;

import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Component
public class ProdutoSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    public ProdutoSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrate() {
        if (!tableExists("produto")) {
            initializeDatabase();
            return;
        }

        boolean addedCashPrice = addColumnIfMissing(
                "valor_venda_vista",
                "ALTER TABLE produto ADD COLUMN valor_venda_vista NUMERIC(10,2) DEFAULT 0"
        );
        boolean addedInstallmentPrice = addColumnIfMissing(
                "valor_venda_prazo",
                "ALTER TABLE produto ADD COLUMN valor_venda_prazo NUMERIC(10,2) DEFAULT 0"
        );
        addColumnIfMissing("foto", "ALTER TABLE produto ADD COLUMN foto BLOB");
        addColumnIfMissing("foto_nome", "ALTER TABLE produto ADD COLUMN foto_nome VARCHAR(180)");
        addColumnIfMissing("foto_tipo", "ALTER TABLE produto ADD COLUMN foto_tipo VARCHAR(80)");
        addColumnIfMissing("foto_tamanho", "ALTER TABLE produto ADD COLUMN foto_tamanho INTEGER");

        if (addedCashPrice) {
            jdbcTemplate.execute("UPDATE produto SET valor_venda_vista = COALESCE(valor_venda, 0)");
        } else {
            jdbcTemplate.execute("UPDATE produto SET valor_venda_vista = COALESCE(valor_venda_vista, valor_venda, 0)");
        }
        if (addedInstallmentPrice) {
            jdbcTemplate.execute("UPDATE produto SET valor_venda_prazo = COALESCE(valor_venda_vista, valor_venda, 0)");
        } else {
            jdbcTemplate.execute("UPDATE produto SET valor_venda_prazo = COALESCE(valor_venda_prazo, valor_venda_vista, valor_venda, 0)");
        }

        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_item_venda_venda ON item_venda(id_venda)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_item_venda_produto ON item_venda(id_produto)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_caixa_venda_tipo ON movimento_caixa(id_venda, tipo)");
    }

    private void initializeDatabase() {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) {
            throw new IllegalStateException("DataSource nao disponivel para inicializar o SQLite.");
        }
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("schema.sql")
        );
        populator.execute(dataSource);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
                Integer.class,
                tableName
        );
        return count != null && count.intValue() > 0;
    }

    private boolean addColumnIfMissing(String columnName, String sql) {
        if (columnExists(columnName)) {
            return false;
        }
        jdbcTemplate.execute(sql);
        return true;
    }

    private boolean columnExists(String columnName) {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(produto)");
        for (Map<String, Object> column : columns) {
            Object name = column.get("name");
            if (name != null && columnName.equalsIgnoreCase(name.toString())) {
                return true;
            }
        }
        return false;
    }
}
