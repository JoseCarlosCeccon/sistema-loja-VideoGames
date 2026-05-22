package com.acaogames.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

public class OrcamentoRequest {

    @NotNull(message = "Ordem de servico e obrigatoria.")
    private Long idOrdemServico;

    @NotBlank(message = "Descricao e obrigatoria.")
    private String descricao;

    @NotNull(message = "Valor estimado e obrigatorio.")
    @PositiveOrZero(message = "Valor estimado deve ser maior ou igual a zero.")
    private BigDecimal valorEstimado;

    private LocalDate dataOrcamento;
    private String statusOrcamento;
    private String observacoes;

    public Long getIdOrdemServico() {
        return idOrdemServico;
    }

    public void setIdOrdemServico(Long idOrdemServico) {
        this.idOrdemServico = idOrdemServico;
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
