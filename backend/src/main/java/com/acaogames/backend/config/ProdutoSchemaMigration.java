package com.acaogames.backend.config;

import javax.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProdutoSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    public ProdutoSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrate() {
        jdbcTemplate.execute("ALTER TABLE produto ADD COLUMN IF NOT EXISTS foto BYTEA");
        jdbcTemplate.execute("ALTER TABLE produto ADD COLUMN IF NOT EXISTS foto_nome VARCHAR(180)");
        jdbcTemplate.execute("ALTER TABLE produto ADD COLUMN IF NOT EXISTS foto_tipo VARCHAR(80)");
        jdbcTemplate.execute("ALTER TABLE produto ADD COLUMN IF NOT EXISTS foto_tamanho INTEGER");
        jdbcTemplate.execute(
                "DO $$ BEGIN "
                        + "IF NOT EXISTS ("
                        + "SELECT 1 FROM pg_constraint WHERE conname = 'chk_produto_foto_tamanho'"
                        + ") THEN "
                        + "ALTER TABLE produto ADD CONSTRAINT chk_produto_foto_tamanho "
                        + "CHECK (foto_tamanho IS NULL OR foto_tamanho <= 512000); "
                        + "END IF; "
                        + "END $$;"
        );
    }
}
