package com.acaogames.backend.controller;

import com.acaogames.backend.dto.OrcamentoRequest;
import com.acaogames.backend.model.Orcamento;
import com.acaogames.backend.service.OrcamentoService;
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
@RequestMapping("/api/orcamentos")
public class OrcamentoController {

    private final OrcamentoService orcamentoService;

    public OrcamentoController(OrcamentoService orcamentoService) {
        this.orcamentoService = orcamentoService;
    }

    @GetMapping
    public List<Orcamento> listar() {
        return orcamentoService.listar();
    }

    @GetMapping("/{idOrcamento}")
    public Orcamento buscar(@PathVariable Long idOrcamento) {
        return orcamentoService.buscar(idOrcamento);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Orcamento criar(@Valid @RequestBody OrcamentoRequest request) {
        return orcamentoService.criar(request);
    }

    @PutMapping("/{idOrcamento}")
    public Orcamento atualizar(@PathVariable Long idOrcamento, @Valid @RequestBody OrcamentoRequest request) {
        return orcamentoService.atualizar(idOrcamento, request);
    }

    @DeleteMapping("/{idOrcamento}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long idOrcamento) {
        orcamentoService.remover(idOrcamento);
    }
}
