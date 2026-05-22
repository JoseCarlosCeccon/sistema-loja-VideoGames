package com.acaogames.backend.service;

import com.acaogames.backend.dto.ItemServicoRequest;
import com.acaogames.backend.exception.ResourceNotFoundException;
import com.acaogames.backend.model.ItemServico;
import com.acaogames.backend.repository.ItemServicoRepository;
import com.acaogames.backend.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemServicoService {

    private final ItemServicoRepository itemServicoRepository;
    private final OrdemServicoService ordemServicoService;
    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueService movimentacaoEstoqueService;

    public ItemServicoService(
            ItemServicoRepository itemServicoRepository,
            OrdemServicoService ordemServicoService,
            ProdutoRepository produtoRepository,
            MovimentacaoEstoqueService movimentacaoEstoqueService
    ) {
        this.itemServicoRepository = itemServicoRepository;
        this.ordemServicoService = ordemServicoService;
        this.produtoRepository = produtoRepository;
        this.movimentacaoEstoqueService = movimentacaoEstoqueService;
    }

    public List<ItemServico> listar() {
        return itemServicoRepository.findAll();
    }

    public List<ItemServico> listarPorOrdemServico(Long idOrdemServico) {
        ordemServicoService.buscar(idOrdemServico);
        return itemServicoRepository.findByOrdemServicoId(idOrdemServico);
    }

    public ItemServico buscar(Long idItemServico) {
        return itemServicoRepository.findById(idItemServico)
                .orElseThrow(notFound("Item de servico nao encontrado."));
    }

    @Transactional
    public ItemServico criar(ItemServicoRequest request) {
        ItemServico item = montarItem(request);
        ItemServico criado = itemServicoRepository.create(item);
        if (Boolean.TRUE.equals(request.getMovimentarEstoque())) {
            movimentacaoEstoqueService.criar(toMovimentacaoSaida(request));
        }
        return criado;
    }

    public ItemServico atualizar(Long idItemServico, ItemServicoRequest request) {
        ItemServico item = buscar(idItemServico);
        preencherItem(item, request);
        return itemServicoRepository.update(item);
    }

    public void remover(Long idItemServico) {
        buscar(idItemServico);
        itemServicoRepository.delete(idItemServico);
    }

    private ItemServico montarItem(ItemServicoRequest request) {
        ItemServico item = new ItemServico();
        preencherItem(item, request);
        return item;
    }

    private void preencherItem(ItemServico item, ItemServicoRequest request) {
        item.setOrdemServico(ordemServicoService.buscar(request.getIdOrdemServico()));
        item.setProduto(produtoRepository.findById(request.getIdProduto())
                .orElseThrow(notFound("Produto informado nao existe.")));
        item.setQuantidade(request.getQuantidade().intValue());
        item.setValorUnitario(request.getValorUnitario());
        item.setSubtotal(request.getSubtotal() == null
                ? request.getValorUnitario().multiply(BigDecimal.valueOf(request.getQuantidade().longValue()))
                : request.getSubtotal());
    }

    private com.acaogames.backend.dto.MovimentacaoEstoqueRequest toMovimentacaoSaida(ItemServicoRequest request) {
        com.acaogames.backend.dto.MovimentacaoEstoqueRequest movimentacao = new com.acaogames.backend.dto.MovimentacaoEstoqueRequest();
        movimentacao.setIdProduto(request.getIdProduto());
        movimentacao.setIdUsuario(request.getIdUsuario());
        movimentacao.setTipo("SAIDA");
        movimentacao.setQuantidade(request.getQuantidade());
        movimentacao.setMotivo("Uso em ordem de servico");
        movimentacao.setObservacoes("Usado na OS " + request.getIdOrdemServico());
        return movimentacao;
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
