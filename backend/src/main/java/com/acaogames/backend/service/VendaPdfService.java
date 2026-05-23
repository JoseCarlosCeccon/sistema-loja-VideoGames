package com.acaogames.backend.service;

import com.acaogames.backend.model.Cliente;
import com.acaogames.backend.model.ItemVenda;
import com.acaogames.backend.model.Produto;
import com.acaogames.backend.model.Venda;
import com.acaogames.backend.repository.ClienteRepository;
import com.acaogames.backend.repository.ProdutoRepository;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class VendaPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;

    public VendaPdfService(ProdutoRepository produtoRepository, ClienteRepository clienteRepository) {
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
    }

    public byte[] gerarPdf(Venda venda) {
        try (InputStream template = new ClassPathResource("reports/comprovante-venda.jrxml").getInputStream()) {
            JasperReport report = JasperCompileManager.compileReport(template);
            JasperPrint print = JasperFillManager.fillReport(
                    report,
                    parametros(venda),
                    new JRMapCollectionDataSource(itens(venda))
            );
            return JasperExportManager.exportReportToPdf(print);
        } catch (Exception exception) {
            throw new IllegalStateException("Nao foi possivel gerar o PDF do comprovante de venda.", exception);
        }
    }

    private Map<String, Object> parametros(Venda venda) throws JRException {
        Cliente cliente = clienteCompleto(venda);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("NUMERO_VENDA", numeroVenda(venda.getIdVenda()));
        params.put("DATA_EMISSAO", formatDate(LocalDate.now()));
        params.put("DATA_VENDA", formatDate(venda.getDataVenda()));
        params.put("CLIENTE_NOME", cliente == null ? "Venda balcao" : value(cliente.getNome()));
        params.put("CLIENTE_TELEFONE", cliente == null ? "Nao informado" : value(cliente.getTelefone()));
        params.put("FORMA_PAGAMENTO", formaPagamento(venda.getFormaPagamento()));
        params.put("VALOR_TOTAL", formatCurrency(venda.getValorTotal()));
        params.put("OBSERVACOES", value(venda.getObservacoes()));
        return params;
    }

    private List<Map<String, ?>> itens(Venda venda) {
        List<Map<String, ?>> rows = new ArrayList<Map<String, ?>>();
        for (ItemVenda item : venda.getItens()) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("ITEM_NOME", produtoNome(item));
            row.put("ITEM_QTD", String.valueOf(item.getQuantidade()));
            row.put("ITEM_VALOR_UNITARIO", formatCurrency(item.getValorUnitario()));
            row.put("ITEM_SUBTOTAL", formatCurrency(item.getSubtotal()));
            rows.add(row);
        }
        return rows;
    }

    private Cliente clienteCompleto(Venda venda) {
        if (venda.getCliente() == null || venda.getCliente().getIdCliente() == null) {
            return null;
        }

        return clienteRepository.findById(venda.getCliente().getIdCliente()).orElse(venda.getCliente());
    }

    private String produtoNome(ItemVenda item) {
        if (item.getProduto() == null || item.getProduto().getIdProduto() == null) {
            return "Produto nao informado";
        }

        Produto produto = produtoRepository.findById(item.getProduto().getIdProduto()).orElse(item.getProduto());
        return value(produto.getNome());
    }

    private String numeroVenda(Long idVenda) {
        return String.format("%06d", idVenda == null ? 0L : idVenda.longValue());
    }

    private String formaPagamento(String formaPagamento) {
        return value(formaPagamento).replace("_", " ");
    }

    private String formatDate(LocalDate date) {
        return date == null ? "Nao informado" : date.format(DATE_FORMATTER);
    }

    private String formatCurrency(BigDecimal value) {
        return value == null ? "Nao informado" : CURRENCY_FORMATTER.format(value);
    }

    private String value(String value) {
        return value == null || value.trim().isEmpty() ? "Nao informado" : value;
    }
}
