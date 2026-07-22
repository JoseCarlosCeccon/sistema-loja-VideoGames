package com.acaogames.backend.config;

import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GarantiaSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    public GarantiaSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrate() {
        if (!tableExists("ordem_servico") || !tableExists("venda")) {
            return;
        }

        boolean addedOrderWarranty = addColumnIfMissing(
                "ordem_servico",
                "garantia_dias",
                "ALTER TABLE ordem_servico ADD COLUMN garantia_dias INTEGER NOT NULL DEFAULT 0 "
                        + "CHECK (garantia_dias IN (0, 30, 90, 365))"
        );
        addColumnIfMissing(
                "venda",
                "garantia_dias",
                "ALTER TABLE venda ADD COLUMN garantia_dias INTEGER NOT NULL DEFAULT 0 "
                        + "CHECK (garantia_dias IN (0, 30, 90, 365))"
        );

        if (addedOrderWarranty && tableExists("garantia")) {
            jdbcTemplate.update(
                    "UPDATE ordem_servico SET garantia_dias = COALESCE(("
                            + "SELECT CASE tempo_garantia_dias "
                            + "WHEN 30 THEN 30 WHEN 90 THEN 90 WHEN 365 THEN 365 ELSE 0 END FROM garantia g "
                            + "WHERE g.id_ordem_servico = ordem_servico.id_ordem_servico"
                            + "), garantia_dias) WHERE garantia_dias = 0"
            );
        }
    }

    private boolean addColumnIfMissing(String table, String column, String sql) {
        if (!columnExists(table, column)) {
            jdbcTemplate.execute(sql);
            return true;
        }
        return false;
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
                Integer.class,
                table
        );
        return count != null && count.intValue() > 0;
    }

    private boolean columnExists(String table, String column) {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(" + table + ")");
        for (Map<String, Object> row : columns) {
            Object name = row.get("name");
            if (name != null && column.equalsIgnoreCase(name.toString())) {
                return true;
            }
        }
        return false;
    }
}
