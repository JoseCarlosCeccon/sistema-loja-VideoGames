package com.acaogames.backend.model;

import java.math.BigDecimal;

public class ItemServico {

    private Long idItemServico;
    private OrdemServico ordemServico;
    private Produto produto;
    private int quantidade;
    private BigDecimal valorUnitario;
    private BigDecimal subtotal;

    public Long getIdItemServico() {
        return idItemServico;
    }

    public void setIdItemServico(Long idItemServico) {
        this.idItemServico = idItemServico;
    }

    public OrdemServico getOrdemServico() {
        return ordemServico;
    }

    public void setOrdemServico(OrdemServico ordemServico) {
        this.ordemServico = ordemServico;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
