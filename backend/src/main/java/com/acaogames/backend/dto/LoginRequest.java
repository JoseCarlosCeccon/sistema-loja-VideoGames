package com.acaogames.backend.dto;

import javax.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Login e obrigatorio.")
    private String login;

    @NotBlank(message = "Senha e obrigatoria.")
    private String senha;

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
}
