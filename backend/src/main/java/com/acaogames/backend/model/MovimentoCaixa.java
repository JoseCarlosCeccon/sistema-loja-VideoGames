package com.acaogames.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MovimentoCaixa {

    private Long idMovimentoCaixa;
    private UsuarioSistema usuario;
    private Venda venda;
    private OrdemServico ordemServico;
    private String tipo;
    private String descricao;
    private BigDecimal valor;
    private LocalDateTime dataMovimento;
    private String formaPagamento;
    private String categoria;

    public Long getIdMovimentoCaixa() {
        return idMovimentoCaixa;
    }

    public void setIdMovimentoCaixa(Long idMovimentoCaixa) {
        this.idMovimentoCaixa = idMovimentoCaixa;
    }

    public UsuarioSistema getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioSistema usuario) {
        this.usuario = usuario;
    }

    public Venda getVenda() {
        return venda;
    }

    public void setVenda(Venda venda) {
        this.venda = venda;
    }

    public OrdemServico getOrdemServico() {
        return ordemServico;
    }

    public void setOrdemServico(OrdemServico ordemServico) {
        this.ordemServico = ordemServico;
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
