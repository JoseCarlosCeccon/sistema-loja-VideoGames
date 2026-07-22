package com.acaogames.backend.service;

import com.acaogames.backend.dto.OrdemServicoRequest;
import com.acaogames.backend.dto.RegistrarSaidaRequest;
import com.acaogames.backend.exception.BadRequestException;
import com.acaogames.backend.exception.ResourceNotFoundException;
import com.acaogames.backend.model.Cliente;
import com.acaogames.backend.model.Equipamento;
import com.acaogames.backend.model.MovimentoCaixa;
import com.acaogames.backend.model.OrdemServico;
import com.acaogames.backend.model.OrdemServicoStatus;
import com.acaogames.backend.model.UsuarioSistema;
import com.acaogames.backend.repository.ClienteRepository;
import com.acaogames.backend.repository.EquipamentoRepository;
import com.acaogames.backend.repository.MovimentoCaixaRepository;
import com.acaogames.backend.repository.OrdemServicoRepository;
import com.acaogames.backend.repository.UsuarioSistemaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdemServicoService {

    private static final Set<String> FORMAS_PAGAMENTO = new HashSet<String>(Arrays.asList(
            "DINHEIRO",
            "PIX",
            "CARTAO_DEBITO",
            "CARTAO_CREDITO"
    ));

    private final OrdemServicoRepository ordemServicoRepository;
    private final ClienteRepository clienteRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final UsuarioSistemaRepository usuarioSistemaRepository;
    private final MovimentoCaixaRepository movimentoCaixaRepository;
    private final Long defaultUserId;

    public OrdemServicoService(
            OrdemServicoRepository ordemServicoRepository,
            ClienteRepository clienteRepository,
            EquipamentoRepository equipamentoRepository,
            UsuarioSistemaRepository usuarioSistemaRepository,
            MovimentoCaixaRepository movimentoCaixaRepository,
            @Value("${app.default-user-id:1}") Long defaultUserId
    ) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.clienteRepository = clienteRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.usuarioSistemaRepository = usuarioSistemaRepository;
        this.movimentoCaixaRepository = movimentoCaixaRepository;
        this.defaultUserId = defaultUserId;
    }

    public List<OrdemServico> listar() {
        return ordemServicoRepository.findAll();
    }

    public OrdemServico buscar(Long idOrdemServico) {
        return ordemServicoRepository.findById(idOrdemServico)
                .orElseThrow(notFound("Ordem de servico nao encontrada."));
    }

    public OrdemServico criar(OrdemServicoRequest request) {
        OrdemServico ordemServico = new OrdemServico();
        preencherOrdemServico(ordemServico, request);
        return ordemServicoRepository.create(ordemServico);
    }

    public OrdemServico atualizar(Long idOrdemServico, OrdemServicoRequest request) {
        OrdemServico ordemServico = buscar(idOrdemServico);
        preencherOrdemServico(ordemServico, request);
        return ordemServicoRepository.update(ordemServico);
    }

    public void remover(Long idOrdemServico) {
        buscar(idOrdemServico);
        ordemServicoRepository.delete(idOrdemServico);
    }

    @Transactional
    public OrdemServico registrarSaida(Long idOrdemServico, RegistrarSaidaRequest request) {
        OrdemServico ordemServico = buscar(idOrdemServico);
        UsuarioSistema usuario = buscarUsuario(request.getIdUsuario());
        BigDecimal valorServico = request.getValorServico() == null ? BigDecimal.ZERO : request.getValorServico();

        ordemServico.setUsuario(usuario);
        ordemServico.setStatus(OrdemServicoStatus.ENTREGUE.name());
        ordemServico.setDataSaida(request.getDataSaida() == null ? LocalDate.now() : request.getDataSaida());
        ordemServico.setDiagnostico(request.getDiagnostico());
        ordemServico.setServicoRealizado(request.getServicoRealizado());
        ordemServico.setValorServico(valorServico);
        ordemServico.setGarantiaDias(GarantiaPrazo.validar(request.getGarantiaDias()));
        ordemServico.setObservacoes(juntarObservacoesSaida(ordemServico.getObservacoes(), request.getObservacoes()));

        OrdemServico atualizada = ordemServicoRepository.update(ordemServico);
        movimentoCaixaRepository.deleteByOrdemServicoId(atualizada.getIdOrdemServico());

        if (valorServico.compareTo(BigDecimal.ZERO) > 0) {
            criarMovimentoCaixaServico(atualizada, usuario, valorServico, request.getFormaPagamento());
        }

        return buscar(atualizada.getIdOrdemServico());
    }

    public List<String> listarStatusDisponiveis() {
        List<String> status = new ArrayList<String>();
        for (OrdemServicoStatus value : OrdemServicoStatus.values()) {
            status.add(value.name());
        }
        return status;
    }

    private void preencherOrdemServico(OrdemServico ordemServico, OrdemServicoRequest request) {
        ordemServico.setCliente(buscarCliente(request.getIdCliente()));
        ordemServico.setEquipamento(buscarEquipamento(request.getIdEquipamento()));
        ordemServico.setUsuario(buscarUsuario(request.getIdUsuario()));
        ordemServico.setNumeroSerie(request.getNumeroSerie());
        ordemServico.setCor(request.getCor());
        ordemServico.setDataEntrada(request.getDataEntrada() == null ? LocalDate.now() : request.getDataEntrada());
        ordemServico.setProblemaRelatado(request.getProblemaRelatado());
        ordemServico.setStatus(normalizarStatus(request.getStatus()));
        ordemServico.setDiagnostico(request.getDiagnostico());
        ordemServico.setServicoRealizado(request.getServicoRealizado());
        ordemServico.setValorServico(request.getValorServico());
        ordemServico.setDataSaida(request.getDataSaida());
        ordemServico.setObservacoes(request.getObservacoes());
    }

    private Cliente buscarCliente(Long idCliente) {
        return clienteRepository.findById(idCliente)
                .orElseThrow(notFound("Cliente informado nao existe."));
    }

    private Equipamento buscarEquipamento(Long idEquipamento) {
        return equipamentoRepository.findById(idEquipamento)
                .orElseThrow(notFound("Equipamento informado nao existe."));
    }

    private UsuarioSistema buscarUsuario(Long idUsuario) {
        Long resolvedId = idUsuario == null ? defaultUserId : idUsuario;
        return usuarioSistemaRepository.findById(resolvedId)
                .orElseThrow(notFound("Usuario informado nao existe."));
    }

    private void criarMovimentoCaixaServico(
            OrdemServico ordemServico,
            UsuarioSistema usuario,
            BigDecimal valorServico,
            String formaPagamento
    ) {
        MovimentoCaixa movimento = new MovimentoCaixa();
        movimento.setUsuario(usuario);
        movimento.setOrdemServico(ordemServico);
        movimento.setTipo("ENTRADA");
        movimento.setDescricao("Pagamento da OS " + ordemServico.getIdOrdemServico());
        movimento.setValor(valorServico);
        movimento.setDataMovimento(LocalDateTime.now());
        movimento.setFormaPagamento(normalizarFormaPagamentoObrigatoria(formaPagamento));
        movimento.setCategoria("SERVICO");
        movimentoCaixaRepository.create(movimento);
    }

    private String normalizarFormaPagamentoObrigatoria(String formaPagamento) {
        String valor = formaPagamento == null ? "" : formaPagamento.trim().toUpperCase();
        if (!FORMAS_PAGAMENTO.contains(valor)) {
            throw new BadRequestException("Forma de pagamento invalida para registrar o valor do servico.");
        }
        return valor;
    }

    private String juntarObservacoesSaida(String observacoesAtuais, String observacoesSaida) {
        if (observacoesSaida == null || observacoesSaida.trim().isEmpty()) {
            return observacoesAtuais;
        }

        if (observacoesAtuais == null || observacoesAtuais.trim().isEmpty()) {
            return "Saida: " + observacoesSaida.trim();
        }

        return observacoesAtuais + "\nSaida: " + observacoesSaida.trim();
    }

    private String normalizarStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return OrdemServicoStatus.RECEBIDO.name();
        }

        String valor = status.trim().toUpperCase();
        try {
            OrdemServicoStatus.valueOf(valor);
            return valor;
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Status de ordem de servico invalido.");
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
