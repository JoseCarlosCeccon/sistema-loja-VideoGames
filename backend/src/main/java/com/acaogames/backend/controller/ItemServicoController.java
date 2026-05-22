package com.acaogames.backend.controller;

import com.acaogames.backend.dto.ItemServicoRequest;
import com.acaogames.backend.model.ItemServico;
import com.acaogames.backend.service.ItemServicoService;
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
@RequestMapping("/api/itens-servico")
public class ItemServicoController {

    private final ItemServicoService itemServicoService;

    public ItemServicoController(ItemServicoService itemServicoService) {
        this.itemServicoService = itemServicoService;
    }

    @GetMapping
    public List<ItemServico> listar() {
        return itemServicoService.listar();
    }

    @GetMapping("/ordem-servico/{idOrdemServico}")
    public List<ItemServico> listarPorOrdemServico(@PathVariable Long idOrdemServico) {
        return itemServicoService.listarPorOrdemServico(idOrdemServico);
    }

    @GetMapping("/{idItemServico}")
    public ItemServico buscar(@PathVariable Long idItemServico) {
        return itemServicoService.buscar(idItemServico);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemServico criar(@Valid @RequestBody ItemServicoRequest request) {
        return itemServicoService.criar(request);
    }

    @PutMapping("/{idItemServico}")
    public ItemServico atualizar(@PathVariable Long idItemServico, @Valid @RequestBody ItemServicoRequest request) {
        return itemServicoService.atualizar(idItemServico, request);
    }

    @DeleteMapping("/{idItemServico}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long idItemServico) {
        itemServicoService.remover(idItemServico);
    }
}
