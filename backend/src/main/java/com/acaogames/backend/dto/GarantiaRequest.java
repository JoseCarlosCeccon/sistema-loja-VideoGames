package com.acaogames.backend.dto;

import java.time.LocalDate;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class GarantiaRequest {

    @NotNull(message = "Ordem de servico e obrigatoria.")
    private Long idOrdemServico;

    @NotNull(message = "Data de inicio e obrigatoria.")
    private LocalDate dataInicio;

    private LocalDate dataFim;

    @NotNull(message = "Tempo de garantia e obrigatorio.")
    @Positive(message = "Tempo de garantia deve ser maior que zero.")
    private Integer tempoGarantiaDias;

    private String descricao;
    private String status;

    public Long getIdOrdemServico() {
        return idOrdemServico;
    }

    public void setIdOrdemServico(Long idOrdemServico) {
        this.idOrdemServico = idOrdemServico;
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

    public Integer getTempoGarantiaDias() {
        return tempoGarantiaDias;
    }

    public void setTempoGarantiaDias(Integer tempoGarantiaDias) {
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
