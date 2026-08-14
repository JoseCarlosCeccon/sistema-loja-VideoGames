package com.acaogames.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acaogames.backend.dto.DashboardSummaryResponse;
import com.acaogames.backend.dto.ItemVendaRequest;
import com.acaogames.backend.dto.VendaRequest;
import com.acaogames.backend.model.MovimentoCaixa;
import com.acaogames.backend.model.Venda;
import com.acaogames.backend.repository.MovimentoCaixaRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/venda-dashboard-integration-test.db?busy_timeout=5000&foreign_keys=on&journal_mode=WAL"
})
class VendaDashboardIntegrationTest {

    private static final Path DATABASE = Paths.get("target", "venda-dashboard-integration-test.db");

    static {
        deleteDatabaseFiles();
    }

    @Autowired
    private VendaService vendaService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private MovimentoCaixaRepository movimentoCaixaRepository;

    @Test
    void deveManterDashboardSincronizadoComOCaixaAoAlterarVenda() {
        Venda venda = vendaService.criar(vendaRequest("DINHEIRO", item(1L, 1, "500.00")));
        assertDashboard("500.00", "500.00");

        venda = vendaService.atualizar(
                venda.getIdVenda(),
                vendaRequest("PIX", item(1L, 1, "400.00"))
        );
        assertMoney("400.00", venda.getValorTotal());
        assertDashboard("400.00", "400.00");

        venda = vendaService.atualizar(
                venda.getIdVenda(),
                vendaRequest("CARTAO_DEBITO", item(1L, 1, "400.00"), item(2L, 1, "100.00"))
        );
        assertMoney("500.00", venda.getValorTotal());
        assertDashboard("500.00", "500.00");

        MovimentoCaixa movimento = movimentoCaixaRepository.findByVendaId(venda.getIdVenda()).orElseThrow(
                () -> new AssertionError("Movimento da venda nao encontrado.")
        );
        assertEquals("CARTAO_DEBITO", movimento.getFormaPagamento());
        assertEquals(LocalDate.now(), movimento.getDataMovimento().toLocalDate());

        movimentoCaixaRepository.delete(movimento.getIdMovimentoCaixa());
        assertTrue(vendaService.buscar(venda.getIdVenda()).getIdVenda() > 0);
        assertDashboard("0.00", "0.00");

        venda = vendaService.atualizar(
                venda.getIdVenda(),
                vendaRequest("PIX", item(1L, 1, "300.00"))
        );
        assertDashboard("300.00", "300.00");

        vendaService.remover(venda.getIdVenda());
        assertDashboard("0.00", "0.00");
    }

    @Test
    void deveUsarSubtotalCalculadoDosItens() {
        ItemVendaRequest item = item(1L, 2, "25.00");
        item.setSubtotal(new BigDecimal("999.00"));

        Venda venda = vendaService.criar(vendaRequest("DINHEIRO", item));

        assertMoney("50.00", venda.getValorTotal());
        assertMoney("50.00", venda.getItens().get(0).getSubtotal());
        vendaService.remover(venda.getIdVenda());
    }

    private void assertDashboard(String vendas, String caixa) {
        DashboardSummaryResponse resumo = dashboardService.buscarResumo();
        assertMoney(vendas, resumo.getVendasDoDia());
        assertMoney(caixa, resumo.getCaixaDoDia());
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private VendaRequest vendaRequest(String formaPagamento, ItemVendaRequest... itens) {
        VendaRequest request = new VendaRequest();
        request.setDataVenda(LocalDate.now());
        request.setFormaPagamento(formaPagamento);
        request.setGarantiaDias(0);
        request.setItens(new ArrayList<ItemVendaRequest>(Arrays.asList(itens)));
        return request;
    }

    private ItemVendaRequest item(Long idProduto, int quantidade, String valorUnitario) {
        ItemVendaRequest item = new ItemVendaRequest();
        item.setIdProduto(idProduto);
        item.setQuantidade(quantidade);
        item.setValorUnitario(new BigDecimal(valorUnitario));
        return item;
    }

    private static void deleteDatabaseFiles() {
        List<Path> files = Arrays.asList(
                DATABASE,
                Paths.get(DATABASE.toString() + "-wal"),
                Paths.get(DATABASE.toString() + "-shm")
        );
        try {
            for (Path file : files) {
                Files.deleteIfExists(file);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel limpar o banco do teste de dashboard.", exception);
        }
    }
}
