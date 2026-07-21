package com.acaogames.backend.service;

import com.acaogames.backend.dto.ProdutoRequest;
import com.acaogames.backend.exception.BadRequestException;
import com.acaogames.backend.exception.ResourceNotFoundException;
import com.acaogames.backend.model.Produto;
import com.acaogames.backend.model.ProdutoFoto;
import com.acaogames.backend.repository.ProdutoRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProdutoService {

    public static final int MAX_FOTO_BYTES = 512000;

        private static final Set<String> TIPOS_IMAGEM_PERMITIDOS = new HashSet<String>(Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
        ));

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

    public Produto salvarFoto(Long idProduto, MultipartFile foto) {
        buscar(idProduto);
        validarFoto(foto);

        try {
            byte[] conteudo = foto.getBytes();
            produtoRepository.updateFoto(
                    idProduto,
                    conteudo,
                    nomeArquivoSeguro(foto.getOriginalFilename()),
                    foto.getContentType(),
                    conteudo.length
            );
        } catch (IOException exception) {
            throw new BadRequestException("Nao foi possivel ler o arquivo da foto.");
        }

        return buscar(idProduto);
    }

    public Produto removerFoto(Long idProduto) {
        buscar(idProduto);
        produtoRepository.deleteFoto(idProduto);
        return buscar(idProduto);
    }

    public ProdutoFoto buscarFoto(Long idProduto) {
        buscar(idProduto);
        return produtoRepository.findFotoById(idProduto)
                .orElseThrow(notFound("Foto do produto nao encontrada."));
    }

    private void preencherProduto(Produto produto, ProdutoRequest request) {
        produto.setNome(request.getNome());
        produto.setCategoria(request.getCategoria());
        produto.setDescricao(request.getDescricao());
        produto.setQuantidadeEstoque(request.getQuantidadeEstoque() == null ? 0 : request.getQuantidadeEstoque().intValue());
        produto.setEstoqueMinimo(request.getEstoqueMinimo() == null ? 0 : request.getEstoqueMinimo().intValue());
        produto.setValorCompra(request.getValorCompra() == null ? BigDecimal.ZERO : request.getValorCompra());
        BigDecimal valorVista = request.getValorVendaVista() != null
                ? request.getValorVendaVista()
                : request.getValorVenda();
        if (valorVista == null) {
            valorVista = BigDecimal.ZERO;
        }
        BigDecimal valorPrazo = request.getValorVendaPrazo() == null ? valorVista : request.getValorVendaPrazo();
        produto.setValorVenda(valorVista);
        produto.setValorVendaVista(valorVista);
        produto.setValorVendaPrazo(valorPrazo);
        produto.setAtivo(request.getAtivo() == null ? true : request.getAtivo().booleanValue());
    }

    private void validarFoto(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new BadRequestException("Informe uma foto para o produto.");
        }

        if (foto.getSize() > MAX_FOTO_BYTES) {
            throw new BadRequestException("A foto deve ter no maximo 500 KB.");
        }

        String contentType = foto.getContentType();
        if (contentType == null || !TIPOS_IMAGEM_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Formato de foto invalido. Use JPG, PNG ou WebP.");
        }
    }

    private String nomeArquivoSeguro(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            return "produto";
        }

        return nome.replaceAll("[\\\\/]", "_");
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
