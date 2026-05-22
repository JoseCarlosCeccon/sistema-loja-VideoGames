package com.acaogames.backend.controller;

import com.acaogames.backend.dto.GarantiaRequest;
import com.acaogames.backend.model.Garantia;
import com.acaogames.backend.service.GarantiaService;
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
@RequestMapping("/api/garantias")
public class GarantiaController {

    private final GarantiaService garantiaService;

    public GarantiaController(GarantiaService garantiaService) {
        this.garantiaService = garantiaService;
    }

    @GetMapping
    public List<Garantia> listar() {
        return garantiaService.listar();
    }

    @GetMapping("/{idGarantia}")
    public Garantia buscar(@PathVariable Long idGarantia) {
        return garantiaService.buscar(idGarantia);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Garantia criar(@Valid @RequestBody GarantiaRequest request) {
        return garantiaService.criar(request);
    }

    @PutMapping("/{idGarantia}")
    public Garantia atualizar(@PathVariable Long idGarantia, @Valid @RequestBody GarantiaRequest request) {
        return garantiaService.atualizar(idGarantia, request);
    }

    @DeleteMapping("/{idGarantia}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long idGarantia) {
        garantiaService.remover(idGarantia);
    }
}
