package com.acaogames.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acaogames.backend.model.Cliente;
import com.acaogames.backend.model.Equipamento;
import com.acaogames.backend.model.OrdemServico;
import com.acaogames.backend.exception.BadRequestException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class OrdemServicoPdfTemplateTest {

    @Test
    void deveCompilarTemplatesJasperDeEntradaESaida() throws Exception {
        String[] templates = {
            "reports/ordem-servico-entrada.jrxml",
            "reports/ordem-servico-saida.jrxml"
        };

        for (String template : templates) {
            try (InputStream inputStream = new ClassPathResource(template).getInputStream()) {
                JasperReport report = JasperCompileManager.compileReport(inputStream);
                assertNotNull(report);
            }
        }
    }

    @Test
    void templateDeEntradaNaoDeveExibirDadosDoServicoConcluido() throws Exception {
        String template = new String(
                Files.readAllBytes(Paths.get("src/main/resources/reports/ordem-servico-entrada.jrxml")),
                StandardCharsets.UTF_8
        );

        assertFalse(template.contains("DIAGNOSTICO"));
        assertFalse(template.contains("SERVICO_REALIZADO"));
        assertFalse(template.contains("VALOR_SERVICO"));
    }

    @Test
    void deveAceitarSomenteOsPrazosDeGarantiaDisponiveis() {
        assertEquals(0, GarantiaPrazo.validar(null));
        assertEquals(30, GarantiaPrazo.validar(30));
        assertEquals(90, GarantiaPrazo.validar(90));
        assertEquals(365, GarantiaPrazo.validar(365));
        assertEquals("1 ano de garantia", GarantiaPrazo.descricao(365));
        assertThrows(BadRequestException.class, () -> GarantiaPrazo.validar(45));
    }

    @Test
    void deveGerarPdfComDadosDaOrdemServico() {
        OrdemServico ordemServico = new OrdemServico();
        Cliente cliente = new Cliente();
        Equipamento equipamento = new Equipamento();

        cliente.setNome("Cliente Teste");
        cliente.setTelefone("(28) 99999-0000");
        equipamento.setMarca("Sony");
        equipamento.setTipoEquipamento("Console");
        equipamento.setModelo("PlayStation 5");

        ordemServico.setIdOrdemServico(1L);
        ordemServico.setCliente(cliente);
        ordemServico.setEquipamento(equipamento);
        ordemServico.setDataEntrada(LocalDate.now());
        ordemServico.setProblemaRelatado("Nao liga.");
        ordemServico.setStatus("RECEBIDO");
        ordemServico.setValorServico(BigDecimal.valueOf(120));

        OrdemServicoPdfService service = new OrdemServicoPdfService();
        byte[] pdf = service.gerarPdf(ordemServico);
        byte[] segundoPdf = service.gerarPdf(ordemServico);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        assertNotNull(segundoPdf);
        assertTrue(segundoPdf.length > 0);

        ordemServico.setDataSaida(LocalDate.now());
        ordemServico.setDiagnostico("Fonte de alimentacao danificada.");
        ordemServico.setServicoRealizado("Substituicao da fonte.");
        ordemServico.setGarantiaDias(90);
        byte[] pdfSaida = service.gerarPdf(ordemServico);

        assertNotNull(pdfSaida);
        assertTrue(pdfSaida.length > 0);
    }
}
