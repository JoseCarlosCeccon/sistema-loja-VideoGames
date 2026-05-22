package com.acaogames.backend.service;

import com.acaogames.backend.dto.ClienteRequest;
import com.acaogames.backend.exception.ResourceNotFoundException;
import com.acaogames.backend.model.Cliente;
import com.acaogames.backend.repository.ClienteRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    public Cliente buscar(Long idCliente) {
        return clienteRepository.findById(idCliente)
                .orElseThrow(functionNotFound("Cliente nao encontrado."));
    }

    public Cliente criar(ClienteRequest request) {
        Cliente cliente = new Cliente();
        preencherCliente(cliente, request);
        return clienteRepository.create(cliente);
    }

    public Cliente atualizar(Long idCliente, ClienteRequest request) {
        Cliente cliente = buscar(idCliente);
        preencherCliente(cliente, request);
        return clienteRepository.update(cliente);
    }

    public void remover(Long idCliente) {
        buscar(idCliente);
        clienteRepository.delete(idCliente);
    }

    private void preencherCliente(Cliente cliente, ClienteRequest request) {
        cliente.setNome(request.getNome());
        cliente.setTelefone(request.getTelefone());
        cliente.setCpf(request.getCpf());
        cliente.setEndereco(request.getEndereco());
        cliente.setEmail(request.getEmail());
        cliente.setObservacoes(request.getObservacoes());
        cliente.setAtivo(request.getAtivo() == null ? true : request.getAtivo().booleanValue());
    }

    private java.util.function.Supplier<ResourceNotFoundException> functionNotFound(final String message) {
        return new java.util.function.Supplier<ResourceNotFoundException>() {
            @Override
            public ResourceNotFoundException get() {
                return new ResourceNotFoundException(message);
            }
        };
    }
}
