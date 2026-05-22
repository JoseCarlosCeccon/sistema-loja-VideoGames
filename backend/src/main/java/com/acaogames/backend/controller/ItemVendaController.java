package com.acaogames.backend.controller;

import com.acaogames.backend.model.ItemVenda;
import com.acaogames.backend.service.ItemVendaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/itens-venda")
public class ItemVendaController {

    private final ItemVendaService itemVendaService;

    public ItemVendaController(ItemVendaService itemVendaService) {
        this.itemVendaService = itemVendaService;
    }

    @GetMapping
    public List<ItemVenda> listar() {
        return itemVendaService.listar();
    }

    @GetMapping("/venda/{idVenda}")
    public List<ItemVenda> listarPorVenda(@PathVariable Long idVenda) {
        return itemVendaService.listarPorVenda(idVenda);
    }

    @GetMapping("/{idItemVenda}")
    public ItemVenda buscar(@PathVariable Long idItemVenda) {
        return itemVendaService.buscar(idItemVenda);
    }
}
