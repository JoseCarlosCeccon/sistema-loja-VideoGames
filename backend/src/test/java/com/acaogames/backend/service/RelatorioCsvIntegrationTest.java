package com.acaogames.backend.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acaogames.backend.dto.ItemVendaRequest;
import com.acaogames.backend.dto.VendaRequest;
import com.acaogames.backend.exception.BadRequestException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/relatorio-csv-integration-test.db?busy_timeout=5000&foreign_keys=on&journal_mode=WAL"
})
class RelatorioCsvIntegrationTest {

    private static final Path DATABASE = Paths.get("target", "relatorio-csv-integration-test.db");

    static {
        try {
            Files.deleteIfExists(DATABASE);
            Files.deleteIfExists(Paths.get(DATABASE.toString() + "-wal"));
            Files.deleteIfExists(Paths.get(DATABASE.toString() + "-shm"));
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel limpar o banco do teste de CSV.", exception);
        }
    }

    @Autowired
    private RelatorioService relatorioService;

    @Autowired
    private VendaService vendaService;

    @Test
    @Transactional
    void deveExportarItensVendidosERespeitarPeriodoSelecionado() {
        LocalDate periodo = LocalDate.of(2035, 4, 2);
        vendaService.criar(vendaRequest(periodo, 2, "19.37"));
        vendaService.criar(vendaRequest(periodo.plusDays(1), 3, "27.41"));

        String csv = relatorioService.movimentacoesCsv(periodo, periodo);

        assertTrue(csv.startsWith("\uFEFF\"Origem\";\"Tipo\";"));
        assertTrue(csv.contains("\"VENDA\";\"ENTRADA\";"));
        assertTrue(csv.contains("\"Cabo HDMI 2.0\""));
        assertTrue(csv.contains("\"19.37\";\"38.74\";\"\""));
        assertTrue(!csv.contains("27.41"));
        assertThrows(
                BadRequestException.class,
                () -> relatorioService.movimentacoesCsv(periodo.plusDays(1), periodo)
        );
    }

    private VendaRequest vendaRequest(LocalDate data, int quantidade, String valorUnitario) {
        ItemVendaRequest item = new ItemVendaRequest();
        item.setIdProduto(1L);
        item.setQuantidade(quantidade);
        item.setValorUnitario(new BigDecimal(valorUnitario));

        VendaRequest request = new VendaRequest();
        request.setDataVenda(data);
        request.setFormaPagamento("PIX");
        request.setGarantiaDias(0);
        request.setItens(Arrays.asList(item));
        return request;
    }
}
