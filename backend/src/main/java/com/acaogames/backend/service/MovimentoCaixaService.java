package com.acaogames.backend.service;

import com.acaogames.backend.dto.MovimentoCaixaRequest;
import com.acaogames.backend.exception.BadRequestException;
import com.acaogames.backend.exception.ResourceNotFoundException;
import com.acaogames.backend.model.MovimentoCaixa;
import com.acaogames.backend.model.OrdemServico;
import com.acaogames.backend.model.UsuarioSistema;
import com.acaogames.backend.model.Venda;
import com.acaogames.backend.repository.MovimentoCaixaRepository;
import com.acaogames.backend.repository.UsuarioSistemaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MovimentoCaixaService {

    private static final Set<String> TIPOS = new HashSet<String>(Arrays.asList("ENTRADA", "SAIDA"));
    private static final Set<String> FORMAS = new HashSet<String>(Arrays.asList(
            "DINHEIRO",
            "PIX",
            "CARTAO_DEBITO",
            "CARTAO_CREDITO"
    ));

    private final MovimentoCaixaRepository movimentoCaixaRepository;
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final VendaService vendaService;
    private final OrdemServicoService ordemServicoService;
    private final Long defaultUserId;

    public MovimentoCaixaService(
            MovimentoCaixaRepository movimentoCaixaRepository,
            UsuarioSistemaRepository usuarioSistemaRepository,
            VendaService vendaService,
            OrdemServicoService ordemServicoService,
            @Value("${app.default-user-id:1}") Long defaultUserId
    ) {
        this.movimentoCaixaRepository = movimentoCaixaRepository;
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.vendaService = vendaService;
        this.ordemServicoService = ordemServicoService;
        this.defaultUserId = defaultUserId;
    }

    public List<MovimentoCaixa> listar() {
        return movimentoCaixaRepository.findAll();
    }

    public MovimentoCaixa buscar(Long idMovimentoCaixa) {
        return movimentoCaixaRepository.findById(idMovimentoCaixa)
                .orElseThrow(notFound("Movimento de caixa nao encontrado."));
    }

    public BigDecimal saldo() {
        return movimentoCaixaRepository.saldo();
    }

    public BigDecimal saldoHoje() {
        return movimentoCaixaRepository.saldoPorData(LocalDate.now());
    }

    public MovimentoCaixa criar(MovimentoCaixaRequest request) {
        MovimentoCaixa movimento = new MovimentoCaixa();
        preencher(movimento, request);
        return movimentoCaixaRepository.create(movimento);
    }

    public MovimentoCaixa atualizar(Long idMovimentoCaixa, MovimentoCaixaRequest request) {
        MovimentoCaixa movimento = buscar(idMovimentoCaixa);
        preencher(movimento, request);
        return movimentoCaixaRepository.update(movimento);
    }

    public void remover(Long idMovimentoCaixa) {
        buscar(idMovimentoCaixa);
        movimentoCaixaRepository.delete(idMovimentoCaixa);
    }

    private void preencher(MovimentoCaixa movimento, MovimentoCaixaRequest request) {
        validarVinculo(request);
        movimento.setUsuario(buscarUsuario(request.getIdUsuario()));
        movimento.setVenda(buscarVendaOpcional(request.getIdVenda()));
        movimento.setOrdemServico(buscarOrdemServicoOpcional(request.getIdOrdemServico()));
        movimento.setTipo(normalizarTipo(request.getTipo()));
        movimento.setDescricao(request.getDescricao());
        movimento.setValor(request.getValor());
        movimento.setDataMovimento(request.getDataMovimento() == null ? LocalDateTime.now() : request.getDataMovimento());
        movimento.setFormaPagamento(normalizarFormaPagamentoOpcional(request.getFormaPagamento()));
        movimento.setCategoria(request.getCategoria());
    }

    private void validarVinculo(MovimentoCaixaRequest request) {
        if (request.getIdVenda() != null && request.getIdOrdemServico() != null) {
            throw new BadRequestException("Movimento de caixa deve estar ligado a uma venda ou a uma OS, nao aos dois.");
        }
    }

    private UsuarioSistema buscarUsuario(Long idUsuario) {
        Long resolvedId = idUsuario == null ? defaultUserId : idUsuario;
        return usuarioSistemaRepository.findById(resolvedId)
                .orElseThrow(notFound("Usuario informado nao existe."));
    }

    private Venda buscarVendaOpcional(Long idVenda) {
        return idVenda == null ? null : vendaService.buscar(idVenda);
    }

    private OrdemServico buscarOrdemServicoOpcional(Long idOrdemServico) {
        return idOrdemServico == null ? null : ordemServicoService.buscar(idOrdemServico);
    }

    private String normalizarTipo(String tipo) {
        String valor = tipo == null ? "" : tipo.trim().toUpperCase();
        if (!TIPOS.contains(valor)) {
            throw new BadRequestException("Tipo de movimento de caixa invalido.");
        }
        return valor;
    }

    private String normalizarFormaPagamentoOpcional(String formaPagamento) {
        if (formaPagamento == null || formaPagamento.trim().isEmpty()) {
            return null;
        }
        String valor = formaPagamento.trim().toUpperCase();
        if (!FORMAS.contains(valor)) {
            throw new BadRequestException("Forma de pagamento invalida.");
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
