package com.acaogames.backend.dto;

import javax.validation.constraints.NotBlank;

public class EquipamentoRequest {

    @NotBlank(message = "Marca e obrigatoria.")
    private String marca;

    @NotBlank(message = "Tipo de equipamento e obrigatorio.")
    private String tipoEquipamento;

    @NotBlank(message = "Modelo e obrigatorio.")
    private String modelo;

    private String descricao;
    private Boolean ativo;

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getTipoEquipamento() {
        return tipoEquipamento;
    }

    public void setTipoEquipamento(String tipoEquipamento) {
        this.tipoEquipamento = tipoEquipamento;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}
