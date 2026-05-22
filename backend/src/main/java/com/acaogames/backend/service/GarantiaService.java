package com.acaogames.backend.service;

import com.acaogames.backend.dto.GarantiaRequest;
import com.acaogames.backend.exception.BadRequestException;
import com.acaogames.backend.exception.ResourceNotFoundException;
import com.acaogames.backend.model.Garantia;
import com.acaogames.backend.repository.GarantiaRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class GarantiaService {

    private static final Set<String> STATUS = new HashSet<String>(Arrays.asList("ATIVA", "EXPIRADA", "CANCELADA"));

    private final GarantiaRepository garantiaRepository;
    private final OrdemServicoService ordemServicoService;

    public GarantiaService(GarantiaRepository garantiaRepository, OrdemServicoService ordemServicoService) {
        this.garantiaRepository = garantiaRepository;
        this.ordemServicoService = ordemServicoService;
    }

    public List<Garantia> listar() {
        return garantiaRepository.findAll();
    }

    public Garantia buscar(Long idGarantia) {
        return garantiaRepository.findById(idGarantia)
                .orElseThrow(notFound("Garantia nao encontrada."));
    }

    public Garantia criar(GarantiaRequest request) {
        Garantia garantia = new Garantia();
        preencher(garantia, request);
        return garantiaRepository.create(garantia);
    }

    public Garantia atualizar(Long idGarantia, GarantiaRequest request) {
        Garantia garantia = buscar(idGarantia);
        preencher(garantia, request);
        return garantiaRepository.update(garantia);
    }

    public void remover(Long idGarantia) {
        buscar(idGarantia);
        garantiaRepository.delete(idGarantia);
    }

    private void preencher(Garantia garantia, GarantiaRequest request) {
        garantia.setOrdemServico(ordemServicoService.buscar(request.getIdOrdemServico()));
        garantia.setDataInicio(request.getDataInicio());
        garantia.setTempoGarantiaDias(request.getTempoGarantiaDias().intValue());
        LocalDate dataFim = request.getDataFim() == null
                ? request.getDataInicio().plusDays(request.getTempoGarantiaDias().intValue())
                : request.getDataFim();
        if (dataFim.isBefore(request.getDataInicio())) {
            throw new BadRequestException("Data final da garantia nao pode ser anterior a data inicial.");
        }
        garantia.setDataFim(dataFim);
        garantia.setDescricao(request.getDescricao());
        garantia.setStatus(normalizarStatus(request.getStatus()));
    }

    private String normalizarStatus(String status) {
        String valor = status == null || status.trim().isEmpty() ? "ATIVA" : status.trim().toUpperCase();
        if (!STATUS.contains(valor)) {
            throw new BadRequestException("Status de garantia invalido.");
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
