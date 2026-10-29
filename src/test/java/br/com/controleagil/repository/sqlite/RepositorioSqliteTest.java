package br.com.controleagil.repository.sqlite;

import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.Produto;
import br.com.controleagil.repository.ClienteRepository;
import br.com.controleagil.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Responsabilidade: testar os repositórios SQLite num banco de verdade, criado numa pasta temporária.
 * Repare que as variáveis são do tipo da INTERFACE (ClienteRepository): o teste não depende do SQLite.
 */
class RepositorioSqliteTest {

    @TempDir
    Path pasta;

    private ProdutoRepository produtos;
    private ClienteRepository clientes;

    @BeforeEach
    void criarBanco() {
        ConexaoBanco banco = new ConexaoBanco("jdbc:sqlite:" + pasta.resolve("teste.db"));
        banco.criarTabelas();
        banco.criarTabelas(); // rodar duas vezes não pode dar erro nem apagar nada
        produtos = new ProdutoRepositorySqlite(banco);
        clientes = new ClienteRepositorySqlite(banco);
    }

    @Test
    @DisplayName("Produto salvo ganha id e volta igual do banco")
    void salvarEBuscarProduto() {
        Produto coxinha = produtos.salvar(new Produto("Coxinha", new BigDecimal("5.50")));

        assertNotNull(coxinha.getId());
        Produto lido = produtos.buscarPorId(coxinha.getId()).orElseThrow();
        assertEquals("Coxinha", lido.getNome());
        assertEquals(new BigDecimal("5.50"), lido.getPreco());
    }

    @Test
    @DisplayName("Salvar de novo atualiza o produto em vez de duplicar")
    void atualizarProduto() {
        Produto coxinha = produtos.salvar(new Produto("Coxinha", new BigDecimal("5.50")));
        coxinha.alterarPreco(new BigDecimal("6.00"));
        produtos.salvar(coxinha);

        assertEquals(1, produtos.listarTodos().size());
        assertEquals(new BigDecimal("6.00"), produtos.buscarPorId(coxinha.getId()).orElseThrow().getPreco());
    }

    @Test
    @DisplayName("Produtos saem em ordem alfabética e id inexistente volta vazio")
    void listarProdutos() {
        produtos.salvar(new Produto("Pastel", new BigDecimal("3.50")));
        produtos.salvar(new Produto("coxinha", new BigDecimal("5.00")));
        produtos.salvar(new Produto("Kibe", new BigDecimal("4.00")));

        List<String> nomes = produtos.listarTodos().stream().map(Produto::getNome).toList();
        assertEquals(List.of("coxinha", "Kibe", "Pastel"), nomes);
        assertTrue(produtos.buscarPorId(999).isEmpty());
    }

    @Test
    @DisplayName("Cliente com conta fiado volta do banco com o mesmo limite")
    void clienteComFiado() {
        Cliente maria = new Cliente("Maria", "(98) 90000-0000", "Rua A, 10");
        maria.abrirContaFiado(new BigDecimal("150.50"));
        clientes.salvar(maria);

        Cliente lida = clientes.buscarPorId(maria.getId()).orElseThrow();
        assertEquals("Maria", lida.getNome());
        assertEquals("Rua A, 10", lida.getEndereco());
        assertTrue(lida.temContaFiado());
        assertEquals(new BigDecimal("150.50"), lida.getContaFiado().getLimiteCredito());
    }

    @Test
    @DisplayName("Abrir conta fiado depois e salvar atualiza o cliente")
    void abrirFiadoDepois() {
        Cliente joao = clientes.salvar(new Cliente("João", null, "Rua B, 25"));
        assertFalse(clientes.buscarPorId(joao.getId()).orElseThrow().temContaFiado());

        joao.abrirContaFiado(new BigDecimal("80.00"));
        clientes.salvar(joao);

        assertEquals(1, clientes.listarTodos().size());
        assertTrue(clientes.buscarPorId(joao.getId()).orElseThrow().temContaFiado());
    }
}
