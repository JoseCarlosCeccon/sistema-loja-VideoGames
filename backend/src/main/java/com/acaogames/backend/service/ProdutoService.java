package com.acaogames.backend.service;

import com.acaogames.backend.dto.ProdutoRequest;
import com.acaogames.backend.exception.ResourceNotFoundException;
import com.acaogames.backend.model.Produto;
import com.acaogames.backend.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public List<Produto> listar() {
        return produtoRepository.findAll();
    }

    public Produto buscar(Long idProduto) {
        return produtoRepository.findById(idProduto)
                .orElseThrow(notFound("Produto nao encontrado."));
    }

    public Produto criar(ProdutoRequest request) {
        Produto produto = new Produto();
        preencherProduto(produto, request);
        return produtoRepository.create(produto);
    }

    public Produto atualizar(Long idProduto, ProdutoRequest request) {
        Produto produto = buscar(idProduto);
        preencherProduto(produto, request);
        return produtoRepository.update(produto);
    }

    public void remover(Long idProduto) {
        buscar(idProduto);
        produtoRepository.delete(idProduto);
    }

    private void preencherProduto(Produto produto, ProdutoRequest request) {
        produto.setNome(request.getNome());
        produto.setCategoria(request.getCategoria());
        produto.setDescricao(request.getDescricao());
        produto.setQuantidadeEstoque(request.getQuantidadeEstoque() == null ? 0 : request.getQuantidadeEstoque().intValue());
        produto.setEstoqueMinimo(request.getEstoqueMinimo() == null ? 0 : request.getEstoqueMinimo().intValue());
        produto.setValorCompra(request.getValorCompra() == null ? BigDecimal.ZERO : request.getValorCompra());
        produto.setValorVenda(request.getValorVenda() == null ? BigDecimal.ZERO : request.getValorVenda());
        produto.setAtivo(request.getAtivo() == null ? true : request.getAtivo().booleanValue());
    }

    private java.util.function.Supplier<ResourceNotFoundException> notFound(final String message) {
        return new java.util.function.Supplier<ResourceNotFoundException>() {
            @Override
            public ResourceNotFoundException get() {
                return new ResourceNotFoundException(message);
            }
        };
    }
}
