package com.acaogames.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acaogames.backend.dto.ItemVendaRequest;
import com.acaogames.backend.dto.RelatorioResponse;
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
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/produto-relatorio-integration-test.db?busy_timeout=5000&foreign_keys=on&journal_mode=WAL"
})
class ProdutoRelatorioIntegrationTest {

    private static final Path DATABASE = Paths.get("target", "produto-relatorio-integration-test.db");

    static {
        deleteDatabaseFiles();
    }

    @Autowired
    private RelatorioService relatorioService;

    @Autowired
    private VendaService vendaService;

    @Autowired
    private MovimentoCaixaRepository movimentoCaixaRepository;

    @Test
    @Transactional
    void deveDistinguirUnidadesVendasProdutosEFaturamentoSemDuplicarVenda() {
        LocalDate data = LocalDate.of(2031, 1, 10);
        Venda vendaA = vendaService.criar(vendaRequest(
                data,
                item(1L, 3, "25.00"),
                item(9L, 1, "40.00")
        ));
        Venda vendaB = vendaService.criar(vendaRequest(data, item(1L, 2, "30.00")));

        RelatorioResponse report = relatorioService.produtos(
                data, data, "", null, "", "COM_VENDAS", "MAIS_UNIDADES", 0, 20
        );

        assertNumber(6, report.getSummary().get("unidadesVendidas"));
        assertNumber(2, report.getSummary().get("vendasRealizadas"));
        assertMoney("175.00", report.getSummary().get("faturamentoProdutos"));
        assertNumber(2, report.getSummary().get("produtosDiferentes"));

        Map<String, Object> caboHdmi = rowById(report, 1L);
        assertNumber(5, caboHdmi.get("unidadesVendidas"));
        assertNumber(2, caboHdmi.get("numeroVendas"));
        assertMoney("27.00", caboHdmi.get("precoMedio"));
        assertMoney("135.00", caboHdmi.get("faturamento"));

        Map<String, Object> primeiroRanking = report.getRankings().get("maisVendidos").get(0);
        assertEquals("Cabo HDMI 2.0", primeiroRanking.get("produto"));
        assertNumber(5, primeiroRanking.get("valor"));

        RelatorioResponse pesquisa = relatorioService.produtos(
                data, data, "Cabo HDMI", null, "", "TODOS", "MAIS_UNIDADES", 0, 20
        );
        assertEquals(1L, pesquisa.getTotalElements());

        RelatorioResponse categoria = relatorioService.produtos(
                data, data, "", null, "Acessorio", "COM_VENDAS", "MAIOR_FATURAMENTO", 0, 1
        );
        assertEquals(2L, categoria.getTotalElements());
        assertEquals(1, categoria.getRows().size());
        assertEquals("Cabo HDMI 2.0", categoria.getRows().get(0).get("produto"));

        RelatorioResponse detalhe = relatorioService.produtoVendas(1L, data, data, 0, 20);
        assertEquals(2L, detalhe.getTotalElements());
        assertNumber(5, detalhe.getSummary().get("unidadesVendidas"));
        assertNumber(2, detalhe.getSummary().get("vendasRealizadas"));
        assertTrue(detalhe.getRows().stream().anyMatch(row -> sameId(row.get("id"), vendaA.getIdVenda())));
        assertTrue(detalhe.getRows().stream().anyMatch(row -> sameId(row.get("id"), vendaB.getIdVenda())));
    }

    @Test
    @Transactional
    void deveRefletirEdicaoPrecoQuantidadeExclusaoEDesvinculacaoFinanceira() {
        LocalDate data = LocalDate.of(2031, 2, 10);
        Venda venda = vendaService.criar(vendaRequest(data, item(2L, 2, "25.00")));
        assertProductMetrics(data, 2L, 2, 1, "50.00", "25.00");

        venda = vendaService.atualizar(venda.getIdVenda(), vendaRequest(data, item(2L, 5, "30.00")));
        assertProductMetrics(data, 2L, 5, 1, "150.00", "30.00");

        MovimentoCaixa movimento = movimentoCaixaRepository.findByVendaId(venda.getIdVenda()).orElseThrow(
                () -> new AssertionError("Movimento financeiro da venda nao encontrado.")
        );
        movimentoCaixaRepository.delete(movimento.getIdMovimentoCaixa());
        RelatorioResponse semMovimento = relatorioService.produtos(
                data, data, "", 2L, "", "COM_VENDAS", "MAIS_UNIDADES", 0, 20
        );
        assertEquals(0L, semMovimento.getTotalElements());
        assertNumber(0, semMovimento.getSummary().get("unidadesVendidas"));

        venda = vendaService.atualizar(venda.getIdVenda(), vendaRequest(data, item(2L, 1, "80.00")));
        assertProductMetrics(data, 2L, 1, 1, "80.00", "80.00");

        vendaService.remover(venda.getIdVenda());
        RelatorioResponse excluida = relatorioService.produtos(
                data, data, "", 2L, "", "COM_VENDAS", "MAIS_UNIDADES", 0, 20
        );
        assertEquals(0L, excluida.getTotalElements());
        assertNumber(0, excluida.getSummary().get("vendasRealizadas"));
    }

    @Test
    @Transactional
    void deveCompararPeriodoAnteriorEquivalenteETratarBaseZero() {
        vendaService.criar(vendaRequest(LocalDate.of(2031, 3, 2), item(1L, 2, "10.00")));
        vendaService.criar(vendaRequest(LocalDate.of(2031, 3, 12), item(1L, 4, "15.00")));

        RelatorioResponse report = relatorioService.produtos(
                LocalDate.of(2031, 3, 11),
                LocalDate.of(2031, 3, 20),
                "",
                null,
                "",
                "COM_VENDAS",
                "MAIS_UNIDADES",
                0,
                20
        );

        assertEquals("2031-03-01", report.getComparison().get("inicioAnterior"));
        assertEquals("2031-03-10", report.getComparison().get("fimAnterior"));
        Map<String, Object> unidades = comparison(report, "unidadesVendidas");
        assertEquals(Boolean.TRUE, unidades.get("disponivel"));
        assertMoney("100.0", unidades.get("percentual"));
        assertEquals("UP", unidades.get("direcao"));

        RelatorioResponse semBase = relatorioService.produtos(
                LocalDate.of(2041, 1, 1),
                LocalDate.of(2041, 1, 10),
                "",
                1L,
                "",
                "TODOS",
                "MAIS_UNIDADES",
                0,
                20
        );
        assertFalse((Boolean) comparison(semBase, "unidadesVendidas").get("disponivel"));
    }

    @Test
    @Transactional
    void deveListarSomenteProdutoAtivoComEstoqueESemVendaNoPeriodo() {
        LocalDate inicio = LocalDate.of(2042, 1, 1);
        LocalDate fim = LocalDate.of(2042, 3, 31);

        RelatorioResponse report = relatorioService.produtos(
                inicio, fim, "", 10L, "", "SEM_VENDAS", "NOME_ASC", 0, 20
        );

        assertEquals(1L, report.getTotalElements());
        assertEquals("Carregador Nintendo Switch", report.getRows().get(0).get("produto"));
        assertNumber(0, report.getRows().get(0).get("unidadesVendidas"));
        assertTrue(((Number) report.getRows().get(0).get("estoqueAtual")).intValue() > 0);
    }

    private void assertProductMetrics(
            LocalDate data,
            Long idProduto,
            int unidades,
            int vendas,
            String faturamento,
            String precoMedio
    ) {
        RelatorioResponse report = relatorioService.produtos(
                data, data, "", idProduto, "", "COM_VENDAS", "MAIS_UNIDADES", 0, 20
        );
        Map<String, Object> row = rowById(report, idProduto);
        assertNumber(unidades, row.get("unidadesVendidas"));
        assertNumber(vendas, row.get("numeroVendas"));
        assertMoney(faturamento, row.get("faturamento"));
        assertMoney(precoMedio, row.get("precoMedio"));
    }

    private VendaRequest vendaRequest(LocalDate data, ItemVendaRequest... itens) {
        VendaRequest request = new VendaRequest();
        request.setDataVenda(data);
        request.setFormaPagamento("PIX");
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

    private Map<String, Object> rowById(RelatorioResponse report, Long idProduto) {
        return report.getRows().stream()
                .filter(row -> sameId(row.get("id"), idProduto))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Produto " + idProduto + " nao encontrado no relatorio."));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> comparison(RelatorioResponse report, String key) {
        return (Map<String, Object>) report.getComparison().get(key);
    }

    private boolean sameId(Object value, Long expected) {
        return value instanceof Number && ((Number) value).longValue() == expected.longValue();
    }

    private void assertNumber(long expected, Object actual) {
        assertEquals(expected, ((Number) actual).longValue());
    }

    private void assertMoney(String expected, Object actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(actual.toString())));
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
            throw new IllegalStateException("Nao foi possivel limpar o banco do teste de relatorio de produtos.", exception);
        }
    }
}
