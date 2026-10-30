package com.acaogames.backend.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RelatorioResponse {

    private String tipo;
    private List<Column> columns = new ArrayList<Column>();
    private List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
    private Map<String, Object> summary = new LinkedHashMap<String, Object>();
    private Map<String, Object> comparison = new LinkedHashMap<String, Object>();
    private Map<String, List<Map<String, Object>>> rankings = new LinkedHashMap<String, List<Map<String, Object>>>();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows;
    }

    public Map<String, Object> getSummary() {
        return summary;
    }

    public void setSummary(Map<String, Object> summary) {
        this.summary = summary;
    }

    public Map<String, Object> getComparison() {
        return comparison;
    }

    public void setComparison(Map<String, Object> comparison) {
        this.comparison = comparison;
    }

    public Map<String, List<Map<String, Object>>> getRankings() {
        return rankings;
    }

    public void setRankings(Map<String, List<Map<String, Object>>> rankings) {
        this.rankings = rankings;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public static class Column {
        private String key;
        private String label;
        private String type;

        public Column() {
        }

        public Column(String key, String label, String type) {
            this.key = key;
            this.label = label;
            this.type = type;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
