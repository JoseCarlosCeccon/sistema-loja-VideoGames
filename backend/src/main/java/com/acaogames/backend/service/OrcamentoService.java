package com.acaogames.backend.service;

import com.acaogames.backend.dto.OrcamentoRequest;
import com.acaogames.backend.exception.BadRequestException;
import com.acaogames.backend.exception.ResourceNotFoundException;
import com.acaogames.backend.model.Orcamento;
import com.acaogames.backend.repository.OrcamentoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OrcamentoService {

    private static final Set<String> STATUS = new HashSet<String>(Arrays.asList("PENDENTE", "APROVADO", "RECUSADO"));

    private final OrcamentoRepository orcamentoRepository;
    private final OrdemServicoService ordemServicoService;

    public OrcamentoService(OrcamentoRepository orcamentoRepository, OrdemServicoService ordemServicoService) {
        this.orcamentoRepository = orcamentoRepository;
        this.ordemServicoService = ordemServicoService;
    }

    public List<Orcamento> listar() {
        return orcamentoRepository.findAll();
    }

    public Orcamento buscar(Long idOrcamento) {
        return orcamentoRepository.findById(idOrcamento)
                .orElseThrow(notFound("Orcamento nao encontrado."));
    }

    public Orcamento criar(OrcamentoRequest request) {
        Orcamento orcamento = new Orcamento();
        preencher(orcamento, request);
        return orcamentoRepository.create(orcamento);
    }

    public Orcamento atualizar(Long idOrcamento, OrcamentoRequest request) {
        Orcamento orcamento = buscar(idOrcamento);
        preencher(orcamento, request);
        return orcamentoRepository.update(orcamento);
    }

    public void remover(Long idOrcamento) {
        buscar(idOrcamento);
        orcamentoRepository.delete(idOrcamento);
    }

    private void preencher(Orcamento orcamento, OrcamentoRequest request) {
        orcamento.setOrdemServico(ordemServicoService.buscar(request.getIdOrdemServico()));
        orcamento.setDescricao(request.getDescricao());
        orcamento.setValorEstimado(request.getValorEstimado() == null ? BigDecimal.ZERO : request.getValorEstimado());
        orcamento.setDataOrcamento(request.getDataOrcamento() == null ? LocalDate.now() : request.getDataOrcamento());
        orcamento.setStatusOrcamento(normalizarStatus(request.getStatusOrcamento()));
        orcamento.setObservacoes(request.getObservacoes());
    }

    private String normalizarStatus(String status) {
        String valor = status == null || status.trim().isEmpty() ? "PENDENTE" : status.trim().toUpperCase();
        if (!STATUS.contains(valor)) {
            throw new BadRequestException("Status de orcamento invalido.");
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
