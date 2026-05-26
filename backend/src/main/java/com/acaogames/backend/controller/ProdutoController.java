package com.acaogames.backend.controller;

import com.acaogames.backend.dto.ProdutoRequest;
import com.acaogames.backend.model.Produto;
import com.acaogames.backend.model.ProdutoFoto;
import com.acaogames.backend.service.ProdutoService;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public List<Produto> listar() {
        return produtoService.listar();
    }

    @GetMapping("/{idProduto}")
    public Produto buscar(@PathVariable Long idProduto) {
        return produtoService.buscar(idProduto);
    }

    @GetMapping("/{idProduto}/foto")
    public ResponseEntity<byte[]> buscarFoto(@PathVariable Long idProduto) {
        ProdutoFoto foto = produtoService.buscarFoto(idProduto);
        String contentType = foto.getTipo() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : foto.getTipo();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(foto.getTamanho()))
                .body(foto.getConteudo());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Produto criar(@Valid @RequestBody ProdutoRequest request) {
        return produtoService.criar(request);
    }

    @PutMapping("/{idProduto}")
    public Produto atualizar(@PathVariable Long idProduto, @Valid @RequestBody ProdutoRequest request) {
        return produtoService.atualizar(idProduto, request);
    }

    @PostMapping(value = "/{idProduto}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Produto salvarFoto(@PathVariable Long idProduto, @RequestParam("foto") MultipartFile foto) {
        return produtoService.salvarFoto(idProduto, foto);
    }

    @DeleteMapping("/{idProduto}/foto")
    public Produto removerFoto(@PathVariable Long idProduto) {
        return produtoService.removerFoto(idProduto);
    }

    @DeleteMapping("/{idProduto}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long idProduto) {
        produtoService.remover(idProduto);
    }
}
