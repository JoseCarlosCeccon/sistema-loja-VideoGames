package com.acaogames.backend.service;

import com.acaogames.backend.dto.ClienteRequest;
import com.acaogames.backend.exception.BadRequestException;
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
        cliente.setNome(trim(request.getNome()));
        cliente.setTelefone(formatTelefone(request.getTelefone()));
        cliente.setCpf(formatCpf(request.getCpf()));
        cliente.setEndereco(optionalText(request.getEndereco()));
        cliente.setEmail(optionalText(request.getEmail()));
        cliente.setObservacoes(optionalText(request.getObservacoes()));
        cliente.setAtivo(request.getAtivo() == null ? true : request.getAtivo().booleanValue());
    }

    private String formatTelefone(String value) {
        String digits = digitsOnly(value);
        if (digits.length() != 11) {
            throw new BadRequestException("Telefone deve conter DDD e 9 digitos no formato (99) 99999-9999.");
        }

        return "(" + digits.substring(0, 2) + ") "
                + digits.substring(2, 7) + "-"
                + digits.substring(7);
    }

    private String formatCpf(String value) {
        String digits = digitsOnly(value);
        if (digits.isEmpty()) {
            return null;
        }

        if (digits.length() != 11) {
            throw new BadRequestException("CPF deve conter 11 digitos no formato 000.000.000-00.");
        }

        return digits.substring(0, 3) + "."
                + digits.substring(3, 6) + "."
                + digits.substring(6, 9) + "-"
                + digits.substring(9);
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String optionalText(String value) {
        String text = trim(value);
        return text.isEmpty() ? null : text;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
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
