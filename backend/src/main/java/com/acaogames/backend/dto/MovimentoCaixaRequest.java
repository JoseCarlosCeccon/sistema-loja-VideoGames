package com.acaogames.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

public class MovimentoCaixaRequest {

    private Long idUsuario;
    private Long idVenda;
    private Long idOrdemServico;

    @NotBlank(message = "Tipo e obrigatorio.")
    private String tipo;

    @NotBlank(message = "Descricao e obrigatoria.")
    private String descricao;

    @NotNull(message = "Valor e obrigatorio.")
    @PositiveOrZero(message = "Valor deve ser maior ou igual a zero.")
    private BigDecimal valor;

    private LocalDateTime dataMovimento;
    private String formaPagamento;

    @NotBlank(message = "Categoria e obrigatoria.")
    private String categoria;

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdVenda() {
        return idVenda;
    }

    public void setIdVenda(Long idVenda) {
        this.idVenda = idVenda;
    }

    public Long getIdOrdemServico() {
        return idOrdemServico;
    }

    public void setIdOrdemServico(Long idOrdemServico) {
        this.idOrdemServico = idOrdemServico;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDateTime getDataMovimento() {
        return dataMovimento;
    }

    public void setDataMovimento(LocalDateTime dataMovimento) {
        this.dataMovimento = dataMovimento;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
}
