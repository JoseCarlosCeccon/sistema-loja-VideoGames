package com.acaogames.backend.dto;

import com.acaogames.backend.model.OrdemServico;
import com.acaogames.backend.model.Venda;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DashboardSummaryResponse {

    private long ordensAbertas;
    private long ordensProntas;
    private long produtosEstoqueBaixo;
    private long clientesAtivos;
    private long produtosCadastrados;
    private BigDecimal valorServicoEmAberto;
    private BigDecimal vendasDoDia;
    private BigDecimal caixaDoDia;
    private BigDecimal saldoCaixa;
    private List<OrdemServico> ordensRecentes = new ArrayList<OrdemServico>();
    private List<Venda> vendasRecentes = new ArrayList<Venda>();
    private List<DashboardStatusCount> ordensPorStatus = new ArrayList<DashboardStatusCount>();

    public long getOrdensAbertas() {
        return ordensAbertas;
    }

    public void setOrdensAbertas(long ordensAbertas) {
        this.ordensAbertas = ordensAbertas;
    }

    public long getOrdensProntas() {
        return ordensProntas;
    }

    public void setOrdensProntas(long ordensProntas) {
        this.ordensProntas = ordensProntas;
    }

    public long getProdutosEstoqueBaixo() {
        return produtosEstoqueBaixo;
    }

    public void setProdutosEstoqueBaixo(long produtosEstoqueBaixo) {
        this.produtosEstoqueBaixo = produtosEstoqueBaixo;
    }

    public long getClientesAtivos() {
        return clientesAtivos;
    }

    public void setClientesAtivos(long clientesAtivos) {
        this.clientesAtivos = clientesAtivos;
    }

    public long getProdutosCadastrados() {
        return produtosCadastrados;
    }

    public void setProdutosCadastrados(long produtosCadastrados) {
        this.produtosCadastrados = produtosCadastrados;
    }

    public BigDecimal getValorServicoEmAberto() {
        return valorServicoEmAberto;
    }

    public void setValorServicoEmAberto(BigDecimal valorServicoEmAberto) {
        this.valorServicoEmAberto = valorServicoEmAberto;
    }

    public BigDecimal getVendasDoDia() {
        return vendasDoDia;
    }

    public void setVendasDoDia(BigDecimal vendasDoDia) {
        this.vendasDoDia = vendasDoDia;
    }

    public BigDecimal getCaixaDoDia() {
        return caixaDoDia;
    }

    public void setCaixaDoDia(BigDecimal caixaDoDia) {
        this.caixaDoDia = caixaDoDia;
    }

    public BigDecimal getSaldoCaixa() {
        return saldoCaixa;
    }

    public void setSaldoCaixa(BigDecimal saldoCaixa) {
        this.saldoCaixa = saldoCaixa;
    }

    public List<OrdemServico> getOrdensRecentes() {
        return ordensRecentes;
    }

    public void setOrdensRecentes(List<OrdemServico> ordensRecentes) {
        this.ordensRecentes = ordensRecentes;
    }

    public List<Venda> getVendasRecentes() {
        return vendasRecentes;
    }

    public void setVendasRecentes(List<Venda> vendasRecentes) {
        this.vendasRecentes = vendasRecentes;
    }

    public List<DashboardStatusCount> getOrdensPorStatus() {
        return ordensPorStatus;
    }

    public void setOrdensPorStatus(List<DashboardStatusCount> ordensPorStatus) {
        this.ordensPorStatus = ordensPorStatus;
    }
}
