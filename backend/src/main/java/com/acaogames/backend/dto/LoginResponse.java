package com.acaogames.backend.dto;

import com.acaogames.backend.model.UsuarioSistema;

public class LoginResponse {

    private Long idUsuario;
    private String nome;
    private String login;
    private String perfil;
    private boolean ativo;

    public static LoginResponse from(UsuarioSistema usuario) {
        LoginResponse response = new LoginResponse();
        response.setIdUsuario(usuario.getIdUsuario());
        response.setNome(usuario.getNome());
        response.setLogin(usuario.getLogin());
        response.setPerfil(usuario.getPerfil());
        response.setAtivo(usuario.isAtivo());
        return response;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

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

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
