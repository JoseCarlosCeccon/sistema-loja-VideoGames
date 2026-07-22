package com.acaogames.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acaogames.backend.model.Cliente;
import com.acaogames.backend.model.Venda;
import com.acaogames.backend.repository.ClienteRepository;
import com.acaogames.backend.service.VendaPdfService;
import com.acaogames.backend.service.VendaService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/sqlite-integration-test.db?busy_timeout=5000&foreign_keys=on&journal_mode=WAL"
})
class SQLiteDatabaseIntegrationTest {

    private static final Path DATABASE = Paths.get("target", "sqlite-integration-test.db");

    static {
        deleteDatabaseFiles();
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VendaService vendaService;

    @Autowired
    private VendaPdfService vendaPdfService;

    @Test
    void deveInicializarBancoEPermitirCrud() {
        Integer foreignKeys = jdbcTemplate.queryForObject("PRAGMA foreign_keys", Integer.class);
        Integer clientesIniciais = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cliente", Integer.class);
        Integer produtosIniciais = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM produto", Integer.class);
        Integer garantiaOrdem = jdbcTemplate.queryForObject(
                "SELECT garantia_dias FROM ordem_servico WHERE id_ordem_servico = 1",
                Integer.class
        );
        Integer garantiaVenda = jdbcTemplate.queryForObject(
                "SELECT garantia_dias FROM venda WHERE id_venda = 1",
                Integer.class
        );

        assertEquals(Integer.valueOf(1), foreignKeys);
        assertEquals(Integer.valueOf(5), clientesIniciais);
        assertEquals(Integer.valueOf(10), produtosIniciais);
        assertEquals(Integer.valueOf(90), garantiaOrdem);
        assertEquals(Integer.valueOf(0), garantiaVenda);

        Cliente cliente = new Cliente();
        cliente.setNome("Teste SQLite");
        cliente.setTelefone("(28) 90000-0000");
        cliente.setAtivo(true);

        Cliente criado = clienteRepository.create(cliente);
        assertNotNull(criado.getIdCliente());
        assertTrue(clienteRepository.findById(criado.getIdCliente()).isPresent());

        clienteRepository.delete(criado.getIdCliente());
    }

    @Test
    void devePreservarOrdemEFormatoDosItensAoListarVendas() {
        List<Venda> vendas = vendaService.listar();

        assertEquals(3, vendas.size());
        assertEquals(Long.valueOf(3L), vendas.get(0).getIdVenda());
        assertEquals(Long.valueOf(2L), vendas.get(1).getIdVenda());
        assertEquals(Long.valueOf(1L), vendas.get(2).getIdVenda());

        assertEquals(1, vendas.get(0).getItens().size());
        assertEquals(2, vendas.get(1).getItens().size());
        assertEquals(1, vendas.get(2).getItens().size());
        assertEquals(Long.valueOf(2L), vendas.get(1).getItens().get(0).getIdItemVenda());
        assertEquals(Long.valueOf(3L), vendas.get(1).getItens().get(1).getIdItemVenda());
        assertEquals(Long.valueOf(7L), vendas.get(1).getItens().get(0).getProduto().getIdProduto());
        assertEquals(Long.valueOf(9L), vendas.get(1).getItens().get(1).getProduto().getIdProduto());
        assertNull(vendas.get(1).getItens().get(0).getProduto().getNome());
    }

    @Test
    void deveGerarComprovanteDeVendaMaisDeUmaVez() {
        Venda venda = vendaService.buscar(2L);

        byte[] primeiroPdf = vendaPdfService.gerarPdf(venda);
        byte[] segundoPdf = vendaPdfService.gerarPdf(venda);

        assertNotNull(primeiroPdf);
        assertTrue(primeiroPdf.length > 0);
        assertNotNull(segundoPdf);
        assertTrue(segundoPdf.length > 0);
    }

    private static void deleteDatabaseFiles() {
        try {
            Files.deleteIfExists(DATABASE);
            Files.deleteIfExists(Paths.get(DATABASE.toString() + "-wal"));
            Files.deleteIfExists(Paths.get(DATABASE.toString() + "-shm"));
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel limpar o banco SQLite de teste.", exception);
        }
    }
}
