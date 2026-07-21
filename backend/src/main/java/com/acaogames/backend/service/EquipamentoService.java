package com.acaogames.backend.service;

import com.acaogames.backend.dto.EquipamentoRequest;
import com.acaogames.backend.exception.ResourceNotFoundException;
import com.acaogames.backend.model.Equipamento;
import com.acaogames.backend.repository.EquipamentoRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EquipamentoService {

    private final EquipamentoRepository equipamentoRepository;

    public EquipamentoService(EquipamentoRepository equipamentoRepository) {
        this.equipamentoRepository = equipamentoRepository;
    }

    public List<Equipamento> listar() {
        return equipamentoRepository.findAll();
    }

    public Equipamento buscar(Long idEquipamento) {
        return equipamentoRepository.findById(idEquipamento)
                .orElseThrow(notFound("Equipamento nao encontrado."));
    }

    public Equipamento criar(EquipamentoRequest request) {
        Equipamento equipamento = new Equipamento();
        preencherEquipamento(equipamento, request);
        return equipamentoRepository.create(equipamento);
    }

    public Equipamento atualizar(Long idEquipamento, EquipamentoRequest request) {
        Equipamento equipamento = buscar(idEquipamento);
        preencherEquipamento(equipamento, request);
        return equipamentoRepository.update(equipamento);
    }

    public void remover(Long idEquipamento) {
        buscar(idEquipamento);
        equipamentoRepository.delete(idEquipamento);
    }

    private void preencherEquipamento(Equipamento equipamento, EquipamentoRequest request) {
        equipamento.setMarca(request.getMarca());
        equipamento.setTipoEquipamento(request.getTipoEquipamento());
        equipamento.setModelo(request.getModelo());
        equipamento.setDescricao(request.getDescricao());
        equipamento.setAtivo(request.getAtivo() == null ? true : request.getAtivo().booleanValue());
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
