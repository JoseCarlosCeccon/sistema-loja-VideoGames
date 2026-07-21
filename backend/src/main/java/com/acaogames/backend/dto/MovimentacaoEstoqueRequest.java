package com.acaogames.backend.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class MovimentacaoEstoqueRequest {

    @NotNull(message = "Produto e obrigatorio.")
    private Long idProduto;

    private Long idUsuario;

    @NotBlank(message = "Tipo e obrigatorio.")
    private String tipo;

    @NotNull(message = "Quantidade e obrigatoria.")
    @Positive(message = "Quantidade deve ser maior que zero.")
    private Integer quantidade;

    private LocalDateTime dataMovimentacao;

    @NotBlank(message = "Motivo e obrigatorio.")
    private String motivo;

    private String observacoes;

    public Long getIdProduto() {
        return idProduto;
    }

    public void setIdProduto(Long idProduto) {
        this.idProduto = idProduto;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public LocalDateTime getDataMovimentacao() {
        return dataMovimentacao;
    }

    public void setDataMovimentacao(LocalDateTime dataMovimentacao) {
        this.dataMovimentacao = dataMovimentacao;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
