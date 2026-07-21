package com.acaogames.backend.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

final class SqliteDateTime {

    private SqliteDateTime() {
    }

    static LocalDateTime readLocalDateTime(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalized = value.trim().replace(' ', 'T');
        if (normalized.length() == 10) {
            return LocalDate.parse(normalized).atStartOfDay();
        }

        try {
            return LocalDateTime.parse(normalized);
        } catch (RuntimeException ignored) {
            return OffsetDateTime.parse(normalized).toLocalDateTime();
        }
    }

    static LocalDate readLocalDate(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.length() > 10) {
            normalized = normalized.substring(0, 10);
        }
        return LocalDate.parse(normalized);
    }
}
