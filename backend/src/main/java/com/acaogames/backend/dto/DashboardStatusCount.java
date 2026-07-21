package com.acaogames.backend.dto;

public class DashboardStatusCount {

    private String status;
    private long total;

    public DashboardStatusCount() {
    }

    public DashboardStatusCount(String status, long total) {
        this.status = status;
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
