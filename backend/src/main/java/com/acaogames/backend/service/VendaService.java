package com.acaogames.backend.service;

import com.acaogames.backend.dto.ItemVendaRequest;
import com.acaogames.backend.dto.VendaRequest;
import com.acaogames.backend.exception.BadRequestException;
import com.acaogames.backend.exception.ResourceNotFoundException;
import com.acaogames.backend.model.Cliente;
import com.acaogames.backend.model.ItemVenda;
import com.acaogames.backend.model.MovimentoCaixa;
import com.acaogames.backend.model.Produto;
import com.acaogames.backend.model.UsuarioSistema;
import com.acaogames.backend.model.Venda;
import com.acaogames.backend.repository.ClienteRepository;
import com.acaogames.backend.repository.ItemVendaRepository;
import com.acaogames.backend.repository.MovimentoCaixaRepository;
import com.acaogames.backend.repository.ProdutoRepository;
import com.acaogames.backend.repository.UsuarioSistemaRepository;
import com.acaogames.backend.repository.VendaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendaService {

    private static final Set<String> FORMAS = new HashSet<String>(Arrays.asList(
            "DINHEIRO",
            "PIX",
            "CARTAO_DEBITO",
            "CARTAO_CREDITO"
    ));

    private final VendaRepository vendaRepository;
    private final ItemVendaRepository itemVendaRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final MovimentacaoEstoqueService movimentacaoEstoqueService;
    private final MovimentoCaixaRepository movimentoCaixaRepository;
    private final Long defaultUserId;

    public VendaService(
            VendaRepository vendaRepository,
            ItemVendaRepository itemVendaRepository,
            ProdutoRepository produtoRepository,
            ClienteRepository clienteRepository,
            UsuarioSistemaRepository usuarioSistemaRepository,
            MovimentacaoEstoqueService movimentacaoEstoqueService,
            MovimentoCaixaRepository movimentoCaixaRepository,
            @Value("${app.default-user-id:1}") Long defaultUserId
    ) {
        this.vendaRepository = vendaRepository;
        this.itemVendaRepository = itemVendaRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.movimentacaoEstoqueService = movimentacaoEstoqueService;
        this.movimentoCaixaRepository = movimentoCaixaRepository;
        this.defaultUserId = defaultUserId;
    }

    public List<Venda> listar() {
        List<Venda> vendas = vendaRepository.findAll();
        preencherItens(vendas);
        return vendas;
    }

    public List<Venda> recentes(int limit) {
        List<Venda> vendas = vendaRepository.findRecent(limit);
        preencherItens(vendas);
        return vendas;
    }

    public Venda buscar(Long idVenda) {
        Venda venda = vendaRepository.findById(idVenda)
                .orElseThrow(notFound("Venda nao encontrada."));
        venda.setItens(itemVendaRepository.findByVendaId(venda.getIdVenda()));
        return venda;
    }

    @Transactional
    public Venda criar(VendaRequest request) {
        UsuarioSistema usuario = buscarUsuario(request.getIdUsuario());
        Venda venda = montarVenda(request);
        venda.setValorTotal(calcularTotal(request.getItens(), request.getValorTotal()));
        Venda criada = vendaRepository.create(venda);
        criarItens(criada, request.getItens(), usuario);
        criarMovimentoCaixa(criada, usuario);
        return buscar(criada.getIdVenda());
    }

    @Transactional
    public Venda atualizar(Long idVenda, VendaRequest request) {
        Venda venda = buscar(idVenda);
        UsuarioSistema usuario = buscarUsuario(request.getIdUsuario());
        devolverEstoque(venda.getItens());
        itemVendaRepository.deleteByVendaId(idVenda);
        movimentoCaixaRepository.deleteByVendaId(idVenda);

        preencherVenda(venda, request);
        venda.setValorTotal(calcularTotal(request.getItens(), request.getValorTotal()));
        Venda atualizada = vendaRepository.update(venda);
        criarItens(atualizada, request.getItens(), usuario);
        criarMovimentoCaixa(atualizada, usuario);
        return buscar(atualizada.getIdVenda());
    }

    @Transactional
    public void remover(Long idVenda) {
        Venda venda = buscar(idVenda);
        devolverEstoque(venda.getItens());
        movimentoCaixaRepository.deleteByVendaId(idVenda);
        vendaRepository.delete(idVenda);
    }

    private Venda montarVenda(VendaRequest request) {
        Venda venda = new Venda();
        preencherVenda(venda, request);
        return venda;
    }

    private void preencherVenda(Venda venda, VendaRequest request) {
        venda.setCliente(buscarClienteOpcional(request.getIdCliente()));
        venda.setDataVenda(request.getDataVenda() == null ? LocalDate.now() : request.getDataVenda());
        venda.setFormaPagamento(normalizarFormaPagamento(request.getFormaPagamento()));
        venda.setObservacoes(request.getObservacoes());
    }

    private void criarItens(Venda venda, List<ItemVendaRequest> itensRequest, UsuarioSistema usuario) {
        List<ItemVenda> itens = new ArrayList<ItemVenda>();
        for (ItemVendaRequest itemRequest : itensRequest) {
            Produto produto = produtoRepository.findById(itemRequest.getIdProduto())
                    .orElseThrow(notFound("Produto informado nao existe."));

            boolean debitou = produtoRepository.debitarEstoque(produto.getIdProduto(), itemRequest.getQuantidade().intValue());
            if (!debitou) {
                throw new BadRequestException("Estoque insuficiente para o produto " + produto.getNome() + ".");
            }

            movimentacaoEstoqueService.registrarSaidaAutomatica(
                    produto.getIdProduto(),
                    usuario.getIdUsuario(),
                    itemRequest.getQuantidade().intValue(),
                    "Venda de produto",
                    "Venda " + venda.getIdVenda()
            );

            ItemVenda item = new ItemVenda();
            item.setVenda(venda);
            item.setProduto(produto);
            item.setQuantidade(itemRequest.getQuantidade().intValue());
            item.setValorUnitario(itemRequest.getValorUnitario());
            item.setSubtotal(calcularSubtotal(itemRequest));
            itens.add(itemVendaRepository.create(item));
        }
        venda.setItens(itens);
    }

    private void criarMovimentoCaixa(Venda venda, UsuarioSistema usuario) {
        MovimentoCaixa movimento = new MovimentoCaixa();
        movimento.setUsuario(usuario);
        movimento.setVenda(venda);
        movimento.setTipo("ENTRADA");
        movimento.setDescricao("Venda " + venda.getIdVenda());
        movimento.setValor(venda.getValorTotal());
        movimento.setDataMovimento(LocalDateTime.now());
        movimento.setFormaPagamento(venda.getFormaPagamento());
        movimento.setCategoria("VENDA");
        movimentoCaixaRepository.create(movimento);
    }

    private void devolverEstoque(List<ItemVenda> itens) {
        for (ItemVenda item : itens) {
            produtoRepository.creditarEstoque(item.getProduto().getIdProduto(), item.getQuantidade());
        }
    }

    private BigDecimal calcularTotal(List<ItemVendaRequest> itens, BigDecimal valorTotalInformado) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemVendaRequest item : itens) {
            total = total.add(calcularSubtotal(item));
        }
        return valorTotalInformado == null ? total : valorTotalInformado;
    }

    private BigDecimal calcularSubtotal(ItemVendaRequest item) {
        return item.getSubtotal() == null
                ? item.getValorUnitario().multiply(BigDecimal.valueOf(item.getQuantidade().longValue()))
                : item.getSubtotal();
    }

    private Cliente buscarClienteOpcional(Long idCliente) {
        return idCliente == null ? null : clienteRepository.findById(idCliente)
                .orElseThrow(notFound("Cliente informado nao existe."));
    }

    private UsuarioSistema buscarUsuario(Long idUsuario) {
        Long resolvedId = idUsuario == null ? defaultUserId : idUsuario;
        return usuarioSistemaRepository.findById(resolvedId)
                .orElseThrow(notFound("Usuario informado nao existe."));
    }

    private String normalizarFormaPagamento(String formaPagamento) {
        String valor = formaPagamento == null ? "" : formaPagamento.trim().toUpperCase();
        if (!FORMAS.contains(valor)) {
            throw new BadRequestException("Forma de pagamento invalida.");
        }
        return valor;
    }

    private void preencherItens(List<Venda> vendas) {
        List<Long> idsVenda = new ArrayList<Long>();
        Map<Long, List<ItemVenda>> itensPorVenda = new LinkedHashMap<Long, List<ItemVenda>>();
        for (Venda venda : vendas) {
            idsVenda.add(venda.getIdVenda());
            itensPorVenda.put(venda.getIdVenda(), new ArrayList<ItemVenda>());
        }

        for (ItemVenda item : itemVendaRepository.findByVendaIds(idsVenda)) {
            List<ItemVenda> itens = itensPorVenda.get(item.getVenda().getIdVenda());
            if (itens != null) {
                itens.add(item);
            }
        }

        for (Venda venda : vendas) {
            venda.setItens(itensPorVenda.get(venda.getIdVenda()));
        }
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
