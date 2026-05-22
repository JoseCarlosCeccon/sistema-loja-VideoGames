package com.acaogames.backend.service;

import com.acaogames.backend.exception.ResourceNotFoundException;
import com.acaogames.backend.model.ItemVenda;
import com.acaogames.backend.repository.ItemVendaRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ItemVendaService {

    private final ItemVendaRepository itemVendaRepository;

    public ItemVendaService(ItemVendaRepository itemVendaRepository) {
        this.itemVendaRepository = itemVendaRepository;
    }

    public List<ItemVenda> listar() {
        return itemVendaRepository.findAll();
    }

    public List<ItemVenda> listarPorVenda(Long idVenda) {
        return itemVendaRepository.findByVendaId(idVenda);
    }

    public ItemVenda buscar(Long idItemVenda) {
        return itemVendaRepository.findById(idItemVenda)
                .orElseThrow(notFound("Item de venda nao encontrado."));
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
