package com.acaogames.backend.dto;

import javax.validation.constraints.NotBlank;

public class UsuarioSistemaRequest {

    @NotBlank(message = "Nome e obrigatorio.")
    private String nome;

    @NotBlank(message = "Login e obrigatorio.")
    private String login;

    @NotBlank(message = "Senha e obrigatoria.")
    private String senha;

    @NotBlank(message = "Perfil e obrigatorio.")
    private String perfil;

    private Boolean ativo;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}
