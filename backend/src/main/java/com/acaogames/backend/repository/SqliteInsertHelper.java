package com.acaogames.backend.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public final class SqliteInsertHelper {

    private SqliteInsertHelper() {
    }

    public static Long insertAndReturnGeneratedKey(
            NamedParameterJdbcTemplate jdbcTemplate,
            String sql,
            SqlParameterSource params
    ) {
        ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
        String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, params);
        Object[] values = NamedParameterUtils.buildValueArray(parsedSql, params, null);

        return jdbcTemplate.getJdbcOperations().execute((Connection connection) -> {
            try (PreparedStatement ps = connection.prepareStatement(sqlToUse)) {
                bindParameters(ps, values);
                ps.executeUpdate();
            }
            try (ResultSet rs = connection.createStatement().executeQuery("SELECT last_insert_rowid()")) {
                return rs.next() ? rs.getLong(1) : null;
            }
        });
    }

    private static void bindParameters(PreparedStatement ps, Object[] values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            int index = i + 1;
            if (value == null) {
                ps.setNull(index, Types.NULL);
            } else if (value instanceof String) {
                ps.setString(index, (String) value);
            } else if (value instanceof Boolean) {
                ps.setBoolean(index, (Boolean) value);
            } else if (value instanceof Integer) {
                ps.setInt(index, (Integer) value);
            } else if (value instanceof Long) {
                ps.setLong(index, (Long) value);
            } else if (value instanceof Short) {
                ps.setShort(index, (Short) value);
            } else if (value instanceof Byte) {
                ps.setByte(index, (Byte) value);
            } else if (value instanceof Double) {
                ps.setDouble(index, (Double) value);
            } else if (value instanceof Float) {
                ps.setFloat(index, (Float) value);
            } else if (value instanceof BigDecimal) {
                ps.setBigDecimal(index, (BigDecimal) value);
            } else if (value instanceof byte[]) {
                ps.setBytes(index, (byte[]) value);
            } else if (value instanceof Date) {
                ps.setDate(index, (Date) value);
            } else if (value instanceof Time) {
                ps.setTime(index, (Time) value);
            } else if (value instanceof Timestamp) {
                ps.setTimestamp(index, (Timestamp) value);
            } else {
                ps.setObject(index, value);
            }
        }
    }
}
