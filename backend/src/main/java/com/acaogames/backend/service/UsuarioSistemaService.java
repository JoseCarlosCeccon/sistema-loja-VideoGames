package com.acaogames.backend.service;

import com.acaogames.backend.dto.LoginRequest;
import com.acaogames.backend.dto.UsuarioSistemaRequest;
import com.acaogames.backend.exception.BadRequestException;
import com.acaogames.backend.exception.ResourceNotFoundException;
import com.acaogames.backend.model.UsuarioSistema;
import com.acaogames.backend.repository.UsuarioSistemaRepository;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UsuarioSistemaService {

    private static final Set<String> PERFIS = new HashSet<String>(Arrays.asList(
            "ADMINISTRADOR",
            "ATENDENTE",
            "TECNICO",
            "FINANCEIRO"
    ));

    private final UsuarioSistemaRepository usuarioSistemaRepository;

    public UsuarioSistemaService(UsuarioSistemaRepository usuarioSistemaRepository) {
        this.usuarioSistemaRepository = usuarioSistemaRepository;
    }

    public List<UsuarioSistema> listar() {
        return usuarioSistemaRepository.findAll();
    }

    public UsuarioSistema buscar(Long idUsuario) {
        return usuarioSistemaRepository.findById(idUsuario)
                .orElseThrow(notFound("Usuario nao encontrado."));
    }

    public UsuarioSistema criar(UsuarioSistemaRequest request) {
        UsuarioSistema usuario = new UsuarioSistema();
        preencherUsuario(usuario, request);
        return usuarioSistemaRepository.create(usuario);
    }

    public UsuarioSistema atualizar(Long idUsuario, UsuarioSistemaRequest request) {
        UsuarioSistema usuario = buscar(idUsuario);
        preencherUsuario(usuario, request);
        return usuarioSistemaRepository.update(usuario);
    }

    public void remover(Long idUsuario) {
        buscar(idUsuario);
        usuarioSistemaRepository.delete(idUsuario);
    }

    public UsuarioSistema login(LoginRequest request) {
        UsuarioSistema usuario = usuarioSistemaRepository.findByLogin(request.getLogin())
                .orElseThrow(notFound("Usuario ou senha invalidos."));

        if (!usuario.isAtivo() || !usuario.getSenha().equals(request.getSenha())) {
            throw new BadRequestException("Usuario ou senha invalidos.");
        }

        return usuario;
    }

    private void preencherUsuario(UsuarioSistema usuario, UsuarioSistemaRequest request) {
        usuario.setNome(request.getNome());
        usuario.setLogin(request.getLogin());
        usuario.setSenha(request.getSenha());
        usuario.setPerfil(normalizarPerfil(request.getPerfil()));
        usuario.setAtivo(request.getAtivo() == null ? true : request.getAtivo().booleanValue());
    }

    private String normalizarPerfil(String perfil) {
        String valor = perfil == null ? "" : perfil.trim().toUpperCase();
        if (!PERFIS.contains(valor)) {
            throw new BadRequestException("Perfil de usuario invalido.");
        }
        return valor;
    }

    private java.util.function.Supplier<ResourceNotFoundException> notFound(final String message) {
        return new java.util.function.Supplier<ResourceNotFoundException>() {
            @Override
            public ResourceNotFoundException get() {
                return new ResourceNotFoundException(message);
            }
        };
    }
}
