package com.acaogames.backend.controller;

import com.acaogames.backend.dto.OrdemServicoRequest;
import com.acaogames.backend.dto.RegistrarSaidaRequest;
import com.acaogames.backend.model.OrdemServico;
import com.acaogames.backend.service.OrdemServicoPdfService;
import com.acaogames.backend.service.OrdemServicoService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/ordens-servico")
public class OrdemServicoController {

    private final OrdemServicoService ordemServicoService;
    private final OrdemServicoPdfService ordemServicoPdfService;

    public OrdemServicoController(
            OrdemServicoService ordemServicoService,
            OrdemServicoPdfService ordemServicoPdfService
    ) {
        this.ordemServicoService = ordemServicoService;
        this.ordemServicoPdfService = ordemServicoPdfService;
    }

    @GetMapping
    public List<OrdemServico> listar() {
        return ordemServicoService.listar();
    }

    @GetMapping("/status")
    public List<String> listarStatus() {
        return ordemServicoService.listarStatusDisponiveis();
    }

    @GetMapping("/{idOrdemServico}")
    public OrdemServico buscar(@PathVariable Long idOrdemServico) {
        return ordemServicoService.buscar(idOrdemServico);
    }

    @GetMapping(value = "/{idOrdemServico}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> gerarPdf(@PathVariable Long idOrdemServico) {
        OrdemServico ordemServico = ordemServicoService.buscar(idOrdemServico);
        byte[] pdf = ordemServicoPdfService.gerarPdf(ordemServico);
        String fileName = "OS-" + String.format("%06d", ordemServico.getIdOrdemServico()) + "-Acao-Games.pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(pdf);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrdemServico criar(@Valid @RequestBody OrdemServicoRequest request) {
        return ordemServicoService.criar(request);
    }

    @PutMapping("/{idOrdemServico}")
    public OrdemServico atualizar(
            @PathVariable Long idOrdemServico,
            @Valid @RequestBody OrdemServicoRequest request
    ) {
        return ordemServicoService.atualizar(idOrdemServico, request);
    }

    @PostMapping("/{idOrdemServico}/saida")
    public OrdemServico registrarSaida(
            @PathVariable Long idOrdemServico,
            @Valid @RequestBody RegistrarSaidaRequest request
    ) {
        return ordemServicoService.registrarSaida(idOrdemServico, request);
    }

    @DeleteMapping("/{idOrdemServico}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long idOrdemServico) {
        ordemServicoService.remover(idOrdemServico);
    }
}
