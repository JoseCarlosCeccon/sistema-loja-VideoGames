package com.acaogames.backend.controller;

import com.acaogames.backend.dto.MovimentacaoEstoqueRequest;
import com.acaogames.backend.model.MovimentacaoEstoque;
import com.acaogames.backend.service.MovimentacaoEstoqueService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movimentacoes-estoque")
public class MovimentacaoEstoqueController {

    private final MovimentacaoEstoqueService movimentacaoEstoqueService;

    public MovimentacaoEstoqueController(MovimentacaoEstoqueService movimentacaoEstoqueService) {
        this.movimentacaoEstoqueService = movimentacaoEstoqueService;
    }

    @GetMapping
    public List<MovimentacaoEstoque> listar() {
        return movimentacaoEstoqueService.listar();
    }

    @GetMapping("/{idMovimentacao}")
    public MovimentacaoEstoque buscar(@PathVariable Long idMovimentacao) {
        return movimentacaoEstoqueService.buscar(idMovimentacao);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoEstoque criar(@Valid @RequestBody MovimentacaoEstoqueRequest request) {
        return movimentacaoEstoqueService.criar(request);
    }

    @DeleteMapping("/{idMovimentacao}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long idMovimentacao) {
        movimentacaoEstoqueService.remover(idMovimentacao);
    }
}
