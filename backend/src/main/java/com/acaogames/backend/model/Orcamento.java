package com.acaogames.backend.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Orcamento {

    private Long idOrcamento;
    private OrdemServico ordemServico;
    private String descricao;
    private BigDecimal valorEstimado;
    private LocalDate dataOrcamento;
    private String statusOrcamento;
    private String observacoes;

    public Long getIdOrcamento() {
        return idOrcamento;
    }

    public void setIdOrcamento(Long idOrcamento) {
        this.idOrcamento = idOrcamento;
    }

    public OrdemServico getOrdemServico() {
        return ordemServico;
    }

    public void setOrdemServico(OrdemServico ordemServico) {
        this.ordemServico = ordemServico;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValorEstimado() {
        return valorEstimado;
    }

    public void setValorEstimado(BigDecimal valorEstimado) {
        this.valorEstimado = valorEstimado;
    }

    public LocalDate getDataOrcamento() {
        return dataOrcamento;
    }

    public void setDataOrcamento(LocalDate dataOrcamento) {
        this.dataOrcamento = dataOrcamento;
    }

    public String getStatusOrcamento() {
        return statusOrcamento;
    }

    public void setStatusOrcamento(String statusOrcamento) {
        this.statusOrcamento = statusOrcamento;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
