package com.acaogames.backend.dto;

import java.math.BigDecimal;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

public class ItemVendaRequest {

    @NotNull(message = "Produto e obrigatorio.")
    private Long idProduto;

    @NotNull(message = "Quantidade e obrigatoria.")
    @Positive(message = "Quantidade deve ser maior que zero.")
    private Integer quantidade;

    @NotNull(message = "Valor unitario e obrigatorio.")
    @PositiveOrZero(message = "Valor unitario deve ser maior ou igual a zero.")
    private BigDecimal valorUnitario;

    private BigDecimal subtotal;

    public Long getIdProduto() {
        return idProduto;
    }

    public void setIdProduto(Long idProduto) {
        this.idProduto = idProduto;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
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
