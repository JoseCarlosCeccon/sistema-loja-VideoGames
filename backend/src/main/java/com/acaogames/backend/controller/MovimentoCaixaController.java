package com.acaogames.backend.controller;

import com.acaogames.backend.dto.MovimentoCaixaRequest;
import com.acaogames.backend.model.MovimentoCaixa;
import com.acaogames.backend.service.MovimentoCaixaService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/movimentos-caixa")
public class MovimentoCaixaController {

    private final MovimentoCaixaService movimentoCaixaService;

    public MovimentoCaixaController(MovimentoCaixaService movimentoCaixaService) {
        this.movimentoCaixaService = movimentoCaixaService;
    }

    @GetMapping
    public List<MovimentoCaixa> listar() {
        return movimentoCaixaService.listar();
    }

    @GetMapping("/saldo")
    public Map<String, BigDecimal> saldo() {
        return Collections.singletonMap("saldo", movimentoCaixaService.saldo());
    }

    @GetMapping("/saldo-hoje")
    public Map<String, BigDecimal> saldoHoje() {
        return Collections.singletonMap("saldo", movimentoCaixaService.saldoHoje());
    }

    @GetMapping("/{idMovimentoCaixa}")
    public MovimentoCaixa buscar(@PathVariable Long idMovimentoCaixa) {
        return movimentoCaixaService.buscar(idMovimentoCaixa);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentoCaixa criar(@Valid @RequestBody MovimentoCaixaRequest request) {
        return movimentoCaixaService.criar(request);
    }

    @PutMapping("/{idMovimentoCaixa}")
    public MovimentoCaixa atualizar(
            @PathVariable Long idMovimentoCaixa,
            @Valid @RequestBody MovimentoCaixaRequest request
    ) {
        return movimentoCaixaService.atualizar(idMovimentoCaixa, request);
    }

    @DeleteMapping("/{idMovimentoCaixa}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long idMovimentoCaixa) {
        movimentoCaixaService.remover(idMovimentoCaixa);
    }
}
