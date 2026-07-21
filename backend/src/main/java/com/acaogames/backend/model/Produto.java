package com.acaogames.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Produto {

    private Long idProduto;
    private String nome;
    private String categoria;
    private String descricao;
    private int quantidadeEstoque;
    private int estoqueMinimo;
    private BigDecimal valorCompra;
    private BigDecimal valorVenda;
    private BigDecimal valorVendaVista;
    private BigDecimal valorVendaPrazo;
    private boolean possuiFoto;
    private String fotoNome;
    private String fotoTipo;
    private Integer fotoTamanho;
    private boolean ativo;
    private LocalDateTime dataCadastro;

    public Long getIdProduto() {
        return idProduto;
    }

    public void setIdProduto(Long idProduto) {
        this.idProduto = idProduto;
    }

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

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public void setQuantidadeEstoque(int quantidadeEstoque) {
        this.quantidadeEstoque = quantidadeEstoque;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(int estoqueMinimo) {
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

    public boolean isPossuiFoto() {
        return possuiFoto;
    }

    public void setPossuiFoto(boolean possuiFoto) {
        this.possuiFoto = possuiFoto;
    }

    public String getFotoNome() {
        return fotoNome;
    }

    public void setFotoNome(String fotoNome) {
        this.fotoNome = fotoNome;
    }

    public String getFotoTipo() {
        return fotoTipo;
    }

    public void setFotoTipo(String fotoTipo) {
        this.fotoTipo = fotoTipo;
    }

    public Integer getFotoTamanho() {
        return fotoTamanho;
    }

    public void setFotoTamanho(Integer fotoTamanho) {
        this.fotoTamanho = fotoTamanho;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }
}
