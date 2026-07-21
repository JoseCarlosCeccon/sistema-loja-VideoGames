package com.acaogames.backend.service;

import com.acaogames.backend.dto.DashboardSummaryResponse;
import com.acaogames.backend.repository.ClienteRepository;
import com.acaogames.backend.repository.MovimentoCaixaRepository;
import com.acaogames.backend.repository.OrdemServicoRepository;
import com.acaogames.backend.repository.ProdutoRepository;
import com.acaogames.backend.repository.VendaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private static final List<String> STATUS_ABERTOS = Arrays.asList(
            "RECEBIDO",
            "EM_ANALISE",
            "AGUARDANDO_APROVACAO",
            "APROVADO",
            "EM_MANUTENCAO"
    );

    private static final List<String> STATUS_PRONTOS = Arrays.asList("PRONTO");

    private final OrdemServicoRepository ordemServicoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final MovimentoCaixaRepository movimentoCaixaRepository;
    private final VendaRepository vendaRepository;
    private final VendaService vendaService;

    public DashboardService(
            OrdemServicoRepository ordemServicoRepository,
            ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository,
            MovimentoCaixaRepository movimentoCaixaRepository,
            VendaRepository vendaRepository,
            VendaService vendaService
    ) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.movimentoCaixaRepository = movimentoCaixaRepository;
        this.vendaRepository = vendaRepository;
        this.vendaService = vendaService;
    }

    public DashboardSummaryResponse buscarResumo() {
        LocalDate hoje = LocalDate.now();
        BigDecimal vendasDoDia = vendaRepository.sumByDate(hoje);
        BigDecimal servicosSaidosHoje = ordemServicoRepository.sumValorServicoByDataSaida(hoje);
        DashboardSummaryResponse resumo = new DashboardSummaryResponse();
        resumo.setOrdensAbertas(ordemServicoRepository.countByStatuses(STATUS_ABERTOS));
        resumo.setOrdensProntas(ordemServicoRepository.countByStatuses(STATUS_PRONTOS));
        resumo.setProdutosEstoqueBaixo(produtoRepository.countLowStock());
        resumo.setClientesAtivos(clienteRepository.countActive());
        resumo.setProdutosCadastrados(produtoRepository.countAll());
        resumo.setValorServicoEmAberto(ordemServicoRepository.sumByStatuses(STATUS_ABERTOS));
        resumo.setVendasDoDia(vendasDoDia);
        resumo.setCaixaDoDia(servicosSaidosHoje.add(vendasDoDia));
        resumo.setSaldoCaixa(movimentoCaixaRepository.saldo());
        resumo.setOrdensRecentes(ordemServicoRepository.findRecent(6));
        resumo.setVendasRecentes(vendaService.recentes(5));
        resumo.setOrdensPorStatus(ordemServicoRepository.countGroupedByStatus());
        return resumo;
    }
}
