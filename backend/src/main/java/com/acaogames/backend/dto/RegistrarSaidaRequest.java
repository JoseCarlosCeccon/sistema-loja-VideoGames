package com.acaogames.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import javax.validation.constraints.PositiveOrZero;

public class RegistrarSaidaRequest {

    private Long idUsuario;
    private LocalDate dataSaida;
    private String diagnostico;
    private String servicoRealizado;

    @PositiveOrZero(message = "Valor do servico deve ser maior ou igual a zero.")
    private BigDecimal valorServico;

    private String formaPagamento;
    private Integer garantiaDias;
    private String observacoes;

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDate getDataSaida() {
        return dataSaida;
    }

    public void setDataSaida(LocalDate dataSaida) {
        this.dataSaida = dataSaida;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getServicoRealizado() {
        return servicoRealizado;
    }

    public void setServicoRealizado(String servicoRealizado) {
        this.servicoRealizado = servicoRealizado;
    }

    public BigDecimal getValorServico() {
        return valorServico;
    }

    public void setValorServico(BigDecimal valorServico) {
        this.valorServico = valorServico;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public Integer getGarantiaDias() {
        return garantiaDias;
    }

    public void setGarantiaDias(Integer garantiaDias) {
        this.garantiaDias = garantiaDias;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
