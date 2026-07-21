package com.acaogames.backend.controller;

import com.acaogames.backend.dto.EquipamentoRequest;
import com.acaogames.backend.model.Equipamento;
import com.acaogames.backend.service.EquipamentoService;
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
@RequestMapping("/api/equipamentos")
public class EquipamentoController {

    private final EquipamentoService equipamentoService;

    public EquipamentoController(EquipamentoService equipamentoService) {
        this.equipamentoService = equipamentoService;
    }

    @GetMapping
    public List<Equipamento> listar() {
        return equipamentoService.listar();
    }

    @GetMapping("/{idEquipamento}")
    public Equipamento buscar(@PathVariable Long idEquipamento) {
        return equipamentoService.buscar(idEquipamento);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Equipamento criar(@Valid @RequestBody EquipamentoRequest request) {
        return equipamentoService.criar(request);
    }

    @PutMapping("/{idEquipamento}")
    public Equipamento atualizar(@PathVariable Long idEquipamento, @Valid @RequestBody EquipamentoRequest request) {
        return equipamentoService.atualizar(idEquipamento, request);
    }

    @DeleteMapping("/{idEquipamento}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long idEquipamento) {
        equipamentoService.remover(idEquipamento);
    }
}
