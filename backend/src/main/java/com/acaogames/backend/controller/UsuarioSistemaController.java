package com.acaogames.backend.controller;

import com.acaogames.backend.dto.LoginRequest;
import com.acaogames.backend.dto.LoginResponse;
import com.acaogames.backend.model.UsuarioSistema;
import com.acaogames.backend.security.AuthTokenService;
import com.acaogames.backend.service.UsuarioSistemaService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioSistemaController {

    private final UsuarioSistemaService usuarioSistemaService;
    private final AuthTokenService authTokenService;

    public UsuarioSistemaController(UsuarioSistemaService usuarioSistemaService, AuthTokenService authTokenService) {
        this.usuarioSistemaService = usuarioSistemaService;
        this.authTokenService = authTokenService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        UsuarioSistema usuario = usuarioSistemaService.login(request);
        return LoginResponse.from(usuario, authTokenService.issueToken(usuario));
    }
}
