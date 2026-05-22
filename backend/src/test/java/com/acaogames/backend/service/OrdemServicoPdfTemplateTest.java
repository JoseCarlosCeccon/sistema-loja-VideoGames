package com.acaogames.backend.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acaogames.backend.model.Cliente;
import com.acaogames.backend.model.Equipamento;
import com.acaogames.backend.model.OrdemServico;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class OrdemServicoPdfTemplateTest {

    @Test
    void deveCompilarTemplateJasper() throws Exception {
        try (InputStream inputStream = new ClassPathResource("reports/ordem-servico.jrxml").getInputStream()) {
            JasperReport report = JasperCompileManager.compileReport(inputStream);
            assertNotNull(report);
        }
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

        byte[] pdf = new OrdemServicoPdfService().gerarPdf(ordemServico);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }
}
