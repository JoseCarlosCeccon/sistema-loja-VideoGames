package com.acaogames.backend.model;

import java.time.LocalDate;

public class Garantia {

    private Long idGarantia;
    private OrdemServico ordemServico;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private int tempoGarantiaDias;
    private String descricao;
    private String status;

    public Long getIdGarantia() {
        return idGarantia;
    }

    public void setIdGarantia(Long idGarantia) {
        this.idGarantia = idGarantia;
    }

    public OrdemServico getOrdemServico() {
        return ordemServico;
    }

    public void setOrdemServico(OrdemServico ordemServico) {
        this.ordemServico = ordemServico;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public int getTempoGarantiaDias() {
        return tempoGarantiaDias;
    }

    public void setTempoGarantiaDias(int tempoGarantiaDias) {
        this.tempoGarantiaDias = tempoGarantiaDias;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
