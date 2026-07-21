package com.acaogames.backend.controller;

import com.acaogames.backend.dto.VendaRequest;
import com.acaogames.backend.model.Venda;
import com.acaogames.backend.service.VendaPdfService;
import com.acaogames.backend.service.VendaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendas")
public class VendaController {

    private final VendaService vendaService;
    private final VendaPdfService vendaPdfService;

    public VendaController(VendaService vendaService, VendaPdfService vendaPdfService) {
        this.vendaService = vendaService;
        this.vendaPdfService = vendaPdfService;
    }

    @GetMapping
    public List<Venda> listar() {
        return vendaService.listar();
    }

    @GetMapping("/recentes")
    public List<Venda> recentes(@RequestParam(defaultValue = "5") int limit) {
        return vendaService.recentes(limit);
    }

    @GetMapping("/{idVenda}")
    public Venda buscar(@PathVariable Long idVenda) {
        return vendaService.buscar(idVenda);
    }

    @GetMapping(value = "/{idVenda}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> gerarPdf(@PathVariable Long idVenda) {
        Venda venda = vendaService.buscar(idVenda);
        byte[] pdf = vendaPdfService.gerarPdf(venda);
        String fileName = "Comprovante-Venda-" + String.format("%06d", venda.getIdVenda()) + "-Acao-Games.pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(pdf);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Venda criar(@Valid @RequestBody VendaRequest request) {
        return vendaService.criar(request);
    }

    @PutMapping("/{idVenda}")
    public Venda atualizar(@PathVariable Long idVenda, @Valid @RequestBody VendaRequest request) {
        return vendaService.atualizar(idVenda, request);
    }

    @DeleteMapping("/{idVenda}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long idVenda) {
        vendaService.remover(idVenda);
    }
}
