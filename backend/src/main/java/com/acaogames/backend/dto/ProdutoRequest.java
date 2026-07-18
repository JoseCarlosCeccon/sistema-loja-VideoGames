package com.acaogames.backend.dto;

import java.math.BigDecimal;
import javax.validation.constraints.NotBlank;

public class ProdutoRequest {

    @NotBlank(message = "Nome e obrigatorio.")
    private String nome;

    @NotBlank(message = "Categoria e obrigatoria.")
    private String categoria;

    private String descricao;
    private Integer quantidadeEstoque;
    private Integer estoqueMinimo;
    private BigDecimal valorCompra;
    private BigDecimal valorVenda;
    private BigDecimal valorVendaVista;
    private BigDecimal valorVendaPrazo;
    private Boolean ativo;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public void setQuantidadeEstoque(Integer quantidadeEstoque) {
        this.quantidadeEstoque = quantidadeEstoque;
    }

    public Integer getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(Integer estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }

    public BigDecimal getValorCompra() {
        return valorCompra;
    }

    public void setValorCompra(BigDecimal valorCompra) {
        this.valorCompra = valorCompra;
    }

    public BigDecimal getValorVenda() {
        return valorVenda;
    }

    public void setValorVenda(BigDecimal valorVenda) {
        this.valorVenda = valorVenda;
    }

    public BigDecimal getValorVendaVista() {
        return valorVendaVista;
    }

    public void setValorVendaVista(BigDecimal valorVendaVista) {
        this.valorVendaVista = valorVendaVista;
    }

    public BigDecimal getValorVendaPrazo() {
        return valorVendaPrazo;
    }

    public void setValorVendaPrazo(BigDecimal valorVendaPrazo) {
        this.valorVendaPrazo = valorVendaPrazo;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}
