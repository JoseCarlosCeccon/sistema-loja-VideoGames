package com.acaogames.backend.controller;

import com.acaogames.backend.dto.ClienteRequest;
import com.acaogames.backend.model.Cliente;
import com.acaogames.backend.service.ClienteService;
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
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<Cliente> listar() {
        return clienteService.listar();
    }

    @GetMapping("/{idCliente}")
    public Cliente buscar(@PathVariable Long idCliente) {
        return clienteService.buscar(idCliente);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Cliente criar(@Valid @RequestBody ClienteRequest request) {
        return clienteService.criar(request);
    }

    @PutMapping("/{idCliente}")
    public Cliente atualizar(@PathVariable Long idCliente, @Valid @RequestBody ClienteRequest request) {
        return clienteService.atualizar(idCliente, request);
    }

    @DeleteMapping("/{idCliente}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long idCliente) {
        clienteService.remover(idCliente);
    }
}
