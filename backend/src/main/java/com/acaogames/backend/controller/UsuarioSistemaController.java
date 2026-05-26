package com.acaogames.backend.controller;

import com.acaogames.backend.dto.LoginRequest;
import com.acaogames.backend.dto.LoginResponse;
import com.acaogames.backend.dto.UsuarioSistemaRequest;
import com.acaogames.backend.model.UsuarioSistema;
import com.acaogames.backend.service.UsuarioSistemaService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioSistemaController {

    private final UsuarioSistemaService usuarioSistemaService;

    public UsuarioSistemaController(UsuarioSistemaService usuarioSistemaService) {
        this.usuarioSistemaService = usuarioSistemaService;
    }

    @GetMapping
    public List<UsuarioSistema> listar() {
        return usuarioSistemaService.listar();
    }

    @GetMapping("/{idUsuario}")
    public UsuarioSistema buscar(@PathVariable Long idUsuario) {
        return usuarioSistemaService.buscar(idUsuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioSistema criar(@Valid @RequestBody UsuarioSistemaRequest request) {
        return usuarioSistemaService.criar(request);
    }

    @PutMapping("/{idUsuario}")
    public UsuarioSistema atualizar(@PathVariable Long idUsuario, @Valid @RequestBody UsuarioSistemaRequest request) {
        return usuarioSistemaService.atualizar(idUsuario, request);
    }

    @DeleteMapping("/{idUsuario}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long idUsuario) {
        usuarioSistemaService.remover(idUsuario);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return LoginResponse.from(usuarioSistemaService.login(request));
    }
}
