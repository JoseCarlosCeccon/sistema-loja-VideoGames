package com.acaogames.backend.service;

import com.acaogames.backend.dto.MovimentacaoEstoqueRequest;
import com.acaogames.backend.exception.BadRequestException;
import com.acaogames.backend.exception.ResourceNotFoundException;
import com.acaogames.backend.model.MovimentacaoEstoque;
import com.acaogames.backend.model.Produto;
import com.acaogames.backend.model.UsuarioSistema;
import com.acaogames.backend.repository.MovimentacaoEstoqueRepository;
import com.acaogames.backend.repository.ProdutoRepository;
import com.acaogames.backend.repository.UsuarioSistemaRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovimentacaoEstoqueService {

    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final Long defaultUserId;

    public MovimentacaoEstoqueService(
            MovimentacaoEstoqueRepository movimentacaoEstoqueRepository,
            ProdutoRepository produtoRepository,
            UsuarioSistemaRepository usuarioSistemaRepository,
            @Value("${app.default-user-id:1}") Long defaultUserId
    ) {
        this.movimentacaoEstoqueRepository = movimentacaoEstoqueRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.defaultUserId = defaultUserId;
    }

    public List<MovimentacaoEstoque> listar() {
        return movimentacaoEstoqueRepository.findAll();
    }

    public MovimentacaoEstoque buscar(Long idMovimentacao) {
        return movimentacaoEstoqueRepository.findById(idMovimentacao)
                .orElseThrow(notFound("Movimentacao de estoque nao encontrada."));
    }

    @Transactional
    public MovimentacaoEstoque criar(MovimentacaoEstoqueRequest request) {
        MovimentacaoEstoque movimentacao = montarMovimentacao(request);
        aplicarMovimentoEstoque(movimentacao.getProduto().getIdProduto(), movimentacao.getTipo(), movimentacao.getQuantidade());
        return movimentacaoEstoqueRepository.create(movimentacao);
    }

    public MovimentacaoEstoque registrarSaidaAutomatica(Long idProduto, Long idUsuario, int quantidade, String motivo, String observacoes) {
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setProduto(buscarProduto(idProduto));
        movimentacao.setUsuario(buscarUsuario(idUsuario));
        movimentacao.setTipo("SAIDA");
        movimentacao.setQuantidade(quantidade);
        movimentacao.setDataMovimentacao(LocalDateTime.now());
        movimentacao.setMotivo(motivo);
        movimentacao.setObservacoes(observacoes);
        return movimentacaoEstoqueRepository.create(movimentacao);
    }

    public void remover(Long idMovimentacao) {
        buscar(idMovimentacao);
        movimentacaoEstoqueRepository.delete(idMovimentacao);
    }

    private MovimentacaoEstoque montarMovimentacao(MovimentacaoEstoqueRequest request) {
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setProduto(buscarProduto(request.getIdProduto()));
        movimentacao.setUsuario(buscarUsuario(request.getIdUsuario()));
        movimentacao.setTipo(normalizarTipo(request.getTipo()));
        movimentacao.setQuantidade(request.getQuantidade().intValue());
        movimentacao.setDataMovimentacao(request.getDataMovimentacao() == null ? LocalDateTime.now() : request.getDataMovimentacao());
        movimentacao.setMotivo(request.getMotivo());
        movimentacao.setObservacoes(request.getObservacoes());
        return movimentacao;
    }

    private void aplicarMovimentoEstoque(Long idProduto, String tipo, int quantidade) {
        if ("ENTRADA".equals(tipo)) {
            produtoRepository.creditarEstoque(idProduto, quantidade);
            return;
        }

        boolean sucesso = produtoRepository.debitarEstoque(idProduto, quantidade);
        if (!sucesso) {
            throw new BadRequestException("Estoque insuficiente para concluir a movimentacao.");
        }
    }

    private Produto buscarProduto(Long idProduto) {
        return produtoRepository.findById(idProduto)
                .orElseThrow(notFound("Produto informado nao existe."));
    }

    private UsuarioSistema buscarUsuario(Long idUsuario) {
        Long resolvedId = idUsuario == null ? defaultUserId : idUsuario;
        return usuarioSistemaRepository.findById(resolvedId)
                .orElseThrow(notFound("Usuario informado nao existe."));
    }

    private String normalizarTipo(String tipo) {
        String valor = tipo == null ? "" : tipo.trim().toUpperCase();
        if (!"ENTRADA".equals(valor) && !"SAIDA".equals(valor)) {
            throw new BadRequestException("Tipo de movimentacao de estoque invalido.");
        }
        return valor;
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
