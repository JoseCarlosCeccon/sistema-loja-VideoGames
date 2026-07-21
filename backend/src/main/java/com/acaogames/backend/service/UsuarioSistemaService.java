package com.acaogames.backend.service;

import com.acaogames.backend.dto.LoginRequest;
import com.acaogames.backend.exception.UnauthorizedException;
import com.acaogames.backend.model.UsuarioSistema;
import com.acaogames.backend.repository.UsuarioSistemaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioSistemaService {

    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioSistemaService(UsuarioSistemaRepository usuarioSistemaRepository, PasswordEncoder passwordEncoder) {
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioSistema login(LoginRequest request) {
        UsuarioSistema usuario = usuarioSistemaRepository.findByLogin(request.getLogin())
                .orElseThrow(unauthorized("Usuario ou senha invalidos."));

        if (!usuario.isAtivo() || !senhaConfere(request.getSenha(), usuario.getSenha())) {
            throw new UnauthorizedException("Usuario ou senha invalidos.");
        }

        return usuario;
    }

    private boolean senhaConfere(String senhaInformada, String senhaArmazenada) {
        if (senhaArmazenada == null) {
            return false;
        }

        if (senhaArmazenada.startsWith("$2a$")
                || senhaArmazenada.startsWith("$2b$")
                || senhaArmazenada.startsWith("$2y$")) {
            return passwordEncoder.matches(senhaInformada, senhaArmazenada);
        }

        return senhaArmazenada.equals(senhaInformada);
    }

    private java.util.function.Supplier<UnauthorizedException> unauthorized(final String message) {
        return new java.util.function.Supplier<UnauthorizedException>() {
            @Override
            public UnauthorizedException get() {
                return new UnauthorizedException(message);
            }
        };
    }
}
