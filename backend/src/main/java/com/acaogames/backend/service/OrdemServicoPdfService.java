package com.acaogames.backend.service;

import com.acaogames.backend.model.Cliente;
import com.acaogames.backend.model.Equipamento;
import com.acaogames.backend.model.OrdemServico;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class OrdemServicoPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ThreadLocal<NumberFormat> CURRENCY_FORMATTER = new ThreadLocal<NumberFormat>() {
        @Override
        protected NumberFormat initialValue() {
            return NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        }
    };

    private volatile JasperReport compiledEntryReport;
    private volatile JasperReport compiledExitReport;

    public byte[] gerarPdf(OrdemServico ordemServico) {
        try {
            JasperReport report = compiledReport(ordemServico.getDataSaida() == null);
            JasperPrint print = JasperFillManager.fillReport(report, parametros(ordemServico), new JREmptyDataSource(1));
            return JasperExportManager.exportReportToPdf(print);
        } catch (Exception exception) {
            throw new IllegalStateException("Nao foi possivel gerar o PDF da ordem de servico.", exception);
        }
    }

    private JasperReport compiledReport(boolean entry) throws Exception {
        JasperReport report = entry ? compiledEntryReport : compiledExitReport;
        if (report == null) {
            synchronized (this) {
                report = entry ? compiledEntryReport : compiledExitReport;
                if (report == null) {
                    String templatePath = entry
                            ? "reports/ordem-servico-entrada.jrxml"
                            : "reports/ordem-servico-saida.jrxml";
                    try (InputStream template = new ClassPathResource(templatePath).getInputStream()) {
                        report = JasperCompileManager.compileReport(template);
                        if (entry) {
                            compiledEntryReport = report;
                        } else {
                            compiledExitReport = report;
                        }
                    }
                }
            }
        }
        return report;
    }

    private Map<String, Object> parametros(OrdemServico ordemServico) throws JRException {
        Cliente cliente = ordemServico.getCliente();
        Equipamento equipamento = ordemServico.getEquipamento();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("NUMERO_OS", numeroOs(ordemServico.getIdOrdemServico()));
        params.put("TIPO_DOCUMENTO", tipoDocumento(ordemServico));
        params.put("DATA_EMISSAO", formatDate(LocalDate.now()));
        params.put("CLIENTE_NOME", value(cliente == null ? null : cliente.getNome()));
        params.put("CLIENTE_TELEFONE", value(cliente == null ? null : cliente.getTelefone()));
        params.put("CLIENTE_CPF", value(cliente == null ? null : cliente.getCpf()));
        params.put("CLIENTE_EMAIL", value(cliente == null ? null : cliente.getEmail()));
        params.put("CLIENTE_ENDERECO", value(cliente == null ? null : cliente.getEndereco()));
        params.put("EQUIPAMENTO", equipamentoLabel(equipamento));
        params.put("NUMERO_SERIE", value(ordemServico.getNumeroSerie()));
        params.put("COR", value(ordemServico.getCor()));
        params.put("STATUS", humanizeStatus(ordemServico.getStatus()));
        params.put("DATA_ENTRADA", formatDate(ordemServico.getDataEntrada()));
        params.put("DATA_SAIDA", formatDate(ordemServico.getDataSaida()));
        params.put("PROBLEMA_RELATADO", value(ordemServico.getProblemaRelatado()));
        params.put("DIAGNOSTICO", value(ordemServico.getDiagnostico()));
        params.put("SERVICO_REALIZADO", value(ordemServico.getServicoRealizado()));
        params.put("VALOR_SERVICO", formatCurrency(ordemServico.getValorServico()));
        params.put("GARANTIA", GarantiaPrazo.descricao(ordemServico.getGarantiaDias()));
        params.put("OBSERVACOES", value(ordemServico.getObservacoes()));
        return params;
    }

    private String tipoDocumento(OrdemServico ordemServico) {
        return ordemServico.getDataSaida() == null ? "ENTRADA PARA MANUTENCAO" : "SAIDA / ENTREGA";
    }

    private String equipamentoLabel(Equipamento equipamento) {
        if (equipamento == null) {
            return "Nao informado";
        }
        return value(equipamento.getMarca()) + " - " + value(equipamento.getTipoEquipamento()) + " - " + value(equipamento.getModelo());
    }

    private String numeroOs(Long idOrdemServico) {
        return String.format("%06d", idOrdemServico == null ? 0L : idOrdemServico.longValue());
    }

    private String humanizeStatus(String status) {
        return value(status).replace("_", " ");
    }

    private String formatDate(LocalDate date) {
        return date == null ? "Nao informado" : date.format(DATE_FORMATTER);
    }

    private String formatCurrency(BigDecimal value) {
        return value == null ? "Nao informado" : CURRENCY_FORMATTER.get().format(value);
    }

    private String value(String value) {
        return value == null || value.trim().isEmpty() ? "Nao informado" : value;
    }
}
