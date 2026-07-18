package com.acaogames.backend.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

final class JdbcTemporalValues {

    private static final DateTimeFormatter DATABASE_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private JdbcTemporalValues() {
    }

    static LocalDate getLocalDate(ResultSet resultSet, String column) throws SQLException {
        Object raw = resultSet.getObject(column);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return fromEpochMillis(((Number) raw).longValue()).toLocalDate();
        }
        String value = raw.toString().trim();
        try {
            if (isInteger(value)) {
                return fromEpochMillis(Long.parseLong(value)).toLocalDate();
            }
            return LocalDate.parse(value.substring(0, 10));
        } catch (RuntimeException exception) {
            throw new SQLException("Data invalida na coluna " + column + ": " + value, exception);
        }
    }

    static LocalDateTime getLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Object raw = resultSet.getObject(column);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return fromEpochMillis(((Number) raw).longValue());
        }
        String value = raw.toString().trim();
        try {
            if (isInteger(value)) {
                return fromEpochMillis(Long.parseLong(value));
            }
            return LocalDateTime.parse(value.replace(' ', 'T'), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException exception) {
            throw new SQLException("Data e hora invalidas na coluna " + column + ": " + value, exception);
        }
    }

    static String toDatabaseValue(LocalDate value) {
        return value == null ? null : value.toString();
    }

    static String toDatabaseValue(LocalDateTime value) {
        return value == null ? null : value.format(DATABASE_DATE_TIME);
    }

    private static LocalDateTime fromEpochMillis(long value) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneId.systemDefault());
    }

    private static boolean isInteger(String value) {
        return value.matches("-?\\d+");
    }
}
