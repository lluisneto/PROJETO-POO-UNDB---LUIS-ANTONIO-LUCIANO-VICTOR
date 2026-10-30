package br.com.controleagil.repository.sqlite;

import br.com.controleagil.exception.RepositorioException;
import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.ContaFiado;
import br.com.controleagil.model.ItemPedido;
import br.com.controleagil.model.MovimentacaoFiado;
import br.com.controleagil.model.PagamentoDinheiro;
import br.com.controleagil.model.PagamentoFiado;
import br.com.controleagil.model.PagamentoPix;
import br.com.controleagil.model.Pedido;
import br.com.controleagil.model.Produto;
import br.com.controleagil.model.StatusPedido;
import br.com.controleagil.model.TipoMovimentacao;
import br.com.controleagil.repository.Repositorios;
import br.com.controleagil.service.ClienteService;
import br.com.controleagil.service.DadosExemplo;
import br.com.controleagil.service.FiadoService;
import br.com.controleagil.service.FormaPagamento;
import br.com.controleagil.service.PedidoService;
import br.com.controleagil.service.ProdutoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static br.com.controleagil.TesteDinheiro.assertValor;
import static br.com.controleagil.TesteDinheiro.reais;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Responsabilidade: testar os repositórios SQLite num banco de verdade, criado numa pasta temporária.
 *
 * A ideia de quase todos os testes é: salvar, "fechar o sistema" (jogar fora os objetos) e
 * REABRIR o banco com BancoSqlite.abrir(), conferindo que tudo voltou igual.
 */
class RepositorioSqliteTest {

    private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 29, 10, 0);
    private static final Clock RELOGIO = Clock.fixed(AGORA.atZone(FUSO).toInstant(), FUSO);

    @TempDir
    Path pasta;

    private ConexaoBanco banco;

    @BeforeEach
    void criarBanco() {
        banco = new ConexaoBanco(pasta.resolve("teste.db"));
    }

    /** "Abre o sistema": lê o banco do zero, como se o programa tivesse acabado de abrir. */
    private Repositorios reabrir() {
        return BancoSqlite.abrir(banco);
    }

    @Test
    @DisplayName("Abrir duas vezes não dá erro nem apaga nada (as tabelas usam IF NOT EXISTS)")
    void abrirDuasVezes() {
        reabrir().produtos().salvar(new Produto("Coxinha", reais("5.50")));
        assertEquals(1, reabrir().produtos().listarTodos().size());
    }

    @Test
    @DisplayName("Produto salvo volta igual; salvar de novo atualiza em vez de duplicar")
    void produtoVoltaIgual() {
        Repositorios repositorios = reabrir();
        Produto coxinha = repositorios.produtos().salvar(new Produto("Coxinha", reais("5.50")));
        repositorios.produtos().salvar(new Produto("bolinha", reais("4.00")));
        coxinha.alterarPreco(reais("6.00"));
        repositorios.produtos().salvar(coxinha);

        List<Produto> lidos = reabrir().produtos().listarTodos();
        assertEquals(List.of("bolinha", "Coxinha"), lidos.stream().map(Produto::getNome).toList());
        assertValor("6.00", lidos.get(1).getPreco());
        assertEquals(coxinha.getId(), lidos.get(1).getId());
    }

    @Test
    @DisplayName("Cliente volta com dados e limite do fiado; id inexistente volta vazio")
    void clienteVoltaIgual() {
        Repositorios repositorios = reabrir();
        Cliente maria = new Cliente("Maria", "(98) 90000-0000", "Rua A, 10");
        maria.abrirContaFiado(reais("150.50"));
        repositorios.clientes().salvar(maria);
        Cliente joao = repositorios.clientes().salvar(new Cliente("João", null, null));

        Repositorios depois = reabrir();
        Cliente mariaLida = depois.clientes().buscarPorId(maria.getId()).orElseThrow();
        assertEquals("Rua A, 10", mariaLida.getEndereco());
        assertValor("150.50", mariaLida.getContaFiado().getLimiteCredito());
        assertFalse(depois.clientes().buscarPorId(joao.getId()).orElseThrow().temContaFiado());
        assertTrue(depois.clientes().buscarPorId(999).isEmpty());
    }

    @Test
    @DisplayName("Pedidos voltam com itens (preço da época), pagamentos, status e o fiado na conta")
    void pedidoCompletoVoltaIgual() {
        Repositorios repositorios = reabrir();
        ClienteService clientes = new ClienteService(repositorios.clientes());
        ProdutoService produtos = new ProdutoService(repositorios.produtos());
        PedidoService pedidos = new PedidoService(repositorios.pedidos(), RELOGIO);
        FiadoService fiado = new FiadoService(repositorios.clientes());

        Produto coxinha = produtos.cadastrar("Coxinha", reais("5.00"));
        Produto kibe = produtos.cadastrar("Kibe", reais("4.00"));
        Cliente bar = clientes.cadastrar("Bar do Zé", "(98) 90000-0001", "Rua B, 2");
        clientes.abrirContaFiado(bar, reais("300.00"));

        List<ItemPedido> itens = List.of(new ItemPedido(coxinha, 10), new ItemPedido(kibe, 5)); // R$ 70,00
        Pedido noFiado = pedidos.registrarPedido(bar, AGORA.plusHours(1), itens,
                pedidos.criarPagamento(FormaPagamento.FIADO, reais("70.00"), null, null));
        pedidos.avancarStatus(noFiado);
        fiado.registrarPagamento(bar, reais("20.00"));

        Pedido dividido = pedidos.registrarPedido(bar, AGORA.plusHours(2), List.of(new ItemPedido(coxinha, 4)), null);
        pedidos.registrarPagamento(dividido, new PagamentoDinheiro(reais("10.00"), reais("15.00")));
        pedidos.registrarPagamento(dividido, new PagamentoPix(reais("5.00"), "E2E0001"));

        Pedido cancelado = pedidos.registrarPedido(bar, AGORA.plusHours(3), List.of(new ItemPedido(kibe, 1)), null);
        pedidos.cancelar(cancelado);

        produtos.atualizar(coxinha, "Coxinha", reais("9.00")); // pedidos antigos mantêm R$ 5,00

        // ---- reabre o "sistema" ----
        Repositorios depois = reabrir();
        Pedido lido = depois.pedidos().buscarPorId(noFiado.getId()).orElseThrow();
        assertEquals(StatusPedido.EM_PRODUCAO, lido.getStatus());
        assertEquals(AGORA.plusHours(1), lido.getDataHoraEntrega());
        assertEquals(AGORA, lido.getDataCriacao());
        assertValor("70.00", lido.calcularTotal());
        assertValor("5.00", lido.getItens().get(0).getPrecoUnitario());
        assertInstanceOf(PagamentoFiado.class, lido.getPagamentos().get(0));

        Pedido divididoLido = depois.pedidos().buscarPorId(dividido.getId()).orElseThrow();
        assertInstanceOf(PagamentoDinheiro.class, divididoLido.getPagamentos().get(0));
        assertValor("5.00", ((PagamentoDinheiro) divididoLido.getPagamentos().get(0)).calcularTroco());
        assertEquals("E2E0001", ((PagamentoPix) divididoLido.getPagamentos().get(1)).getCodigoTransacao());
        assertValor("15.00", divididoLido.calcularValorPago());

        assertEquals(StatusPedido.CANCELADO, depois.pedidos().buscarPorId(cancelado.getId()).orElseThrow().getStatus());

        // O cliente do pedido é o MESMO objeto que está na lista de clientes (nada duplicado).
        Cliente barLido = depois.clientes().buscarPorId(bar.getId()).orElseThrow();
        assertSame(barLido, lido.getCliente());

        // Fiado: compra de 70 e pagamento de 20, nessa ordem, com a compra ligada ao pedido.
        ContaFiado conta = barLido.getContaFiado();
        assertValor("50.00", conta.getSaldoDevedor());
        List<MovimentacaoFiado> historico = conta.getMovimentacoes();
        assertEquals(TipoMovimentacao.COMPRA, historico.get(0).getTipo());
        assertSame(lido, historico.get(0).getPedido());
        assertEquals(TipoMovimentacao.PAGAMENTO, historico.get(1).getTipo());
        assertNull(historico.get(1).getPedido());
        assertNotNull(historico.get(1).getData());
    }

    @Test
    @DisplayName("Se a gravação falha no meio, nada fica salvo pela metade (transação)")
    void transacaoDesfazTudo() {
        Repositorios repositorios = reabrir();
        Cliente maria = repositorios.clientes().salvar(new Cliente("Maria", null, null));
        Produto naoSalvo = new Produto("Coxinha", reais("5.00")); // sem id: o item não pode ser gravado
        Pedido pedido = new Pedido(maria, AGORA.plusHours(1));
        pedido.adicionarItem(naoSalvo, 2);

        assertThrows(RepositorioException.class, () -> repositorios.pedidos().salvar(pedido));
        assertNull(pedido.getId(), "o pedido não ganha número se não foi gravado");
        assertTrue(reabrir().pedidos().listarTodos().isEmpty());
    }

    @Test
    @DisplayName("Pedido no fiado e a compra na conta são gravados juntos: se um falha, nenhum fica")
    void pedidoEFiadoNaMesmaTransacao() throws Exception {
        Repositorios repositorios = reabrir();
        ClienteService clientes = new ClienteService(repositorios.clientes());
        Produto coxinha = new ProdutoService(repositorios.produtos()).cadastrar("Coxinha", reais("5.00"));
        Cliente bar = clientes.cadastrar("Bar do Zé", null, null);
        clientes.abrirContaFiado(bar, reais("300.00"));
        PedidoService pedidos = new PedidoService(repositorios.pedidos(), RELOGIO);

        // Faz o banco recusar qualquer linha nova no histórico do fiado (simula uma falha no meio).
        executar("CREATE TRIGGER falha_de_teste BEFORE INSERT ON movimentacao_fiado BEGIN SELECT RAISE(ABORT, 'falha de teste'); END");
        List<ItemPedido> itens = List.of(new ItemPedido(coxinha, 10));
        assertThrows(RepositorioException.class, () -> pedidos.registrarPedido(bar, AGORA.plusHours(1), itens,
                pedidos.criarPagamento(FormaPagamento.FIADO, reais("50.00"), null, null)));
        executar("DROP TRIGGER falha_de_teste");

        Repositorios depois = reabrir();
        assertTrue(depois.pedidos().listarTodos().isEmpty(), "o pedido não pode ficar salvo sem a compra");
        assertValor("0", depois.clientes().buscarPorId(bar.getId()).orElseThrow().getContaFiado().getSaldoDevedor());
    }

    @Test
    @DisplayName("Cancelamento com estorno e limite alterado voltam iguais do banco")
    void estornoELimiteVoltamIguais() {
        Repositorios repositorios = reabrir();
        ClienteService clientes = new ClienteService(repositorios.clientes());
        Produto coxinha = new ProdutoService(repositorios.produtos()).cadastrar("Coxinha", reais("5.00"));
        Cliente bar = clientes.cadastrar("Bar do Zé", null, null);
        clientes.abrirContaFiado(bar, reais("100.00"));
        PedidoService pedidos = new PedidoService(repositorios.pedidos(), RELOGIO);
        Pedido pedido = pedidos.registrarPedido(bar, AGORA.plusHours(1), List.of(new ItemPedido(coxinha, 4)),
                pedidos.criarPagamento(FormaPagamento.FIADO, reais("20.00"), null, null));
        pedidos.cancelar(pedido);
        clientes.alterarLimiteFiado(bar, reais("250.00"));

        Cliente lido = reabrir().clientes().buscarPorId(bar.getId()).orElseThrow();
        assertValor("0", lido.getContaFiado().getSaldoDevedor());
        assertEquals(TipoMovimentacao.ESTORNO, lido.getContaFiado().getMovimentacoes().get(1).getTipo());
        assertValor("250.00", lido.getContaFiado().getLimiteCredito());
    }

    @Test
    @DisplayName("Banco de uma versão antiga (sem estorno) é atualizado sozinho, sem perder dados")
    void migraBancoAntigo() throws Exception {
        // Monta um banco "da versão antiga": a tabela do fiado só aceita COMPRA e PAGAMENTO.
        executar("CREATE TABLE cliente (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL, telefone TEXT,"
                + " endereco TEXT, limite_fiado TEXT)");
        executar("CREATE TABLE pedido (id INTEGER PRIMARY KEY AUTOINCREMENT, cliente_id INTEGER NOT NULL REFERENCES cliente (id),"
                + " data_criacao TEXT NOT NULL, data_hora_entrega TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'PENDENTE')");
        executar("CREATE TABLE movimentacao_fiado (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " cliente_id INTEGER NOT NULL REFERENCES cliente (id),"
                + " tipo TEXT NOT NULL CHECK (tipo IN ('COMPRA', 'PAGAMENTO')), valor TEXT NOT NULL, data TEXT NOT NULL,"
                + " pedido_id INTEGER REFERENCES pedido (id))");
        executar("INSERT INTO cliente (nome, limite_fiado) VALUES ('Maria', '100.00')");
        executar("INSERT INTO movimentacao_fiado (cliente_id, tipo, valor, data) VALUES (1, 'COMPRA', '30.00', '2026-10-28T10:00')");

        Repositorios repositorios = reabrir(); // aqui acontece a migração
        Cliente maria = repositorios.clientes().buscarPorId(1).orElseThrow();
        assertValor("30.00", maria.getContaFiado().getSaldoDevedor(), "o histórico antigo continua lá");

        maria.getContaFiado().registrarEstorno(reais("10.00"), null);
        repositorios.clientes().salvar(maria); // na versão antiga, ESTORNO seria recusado pelo banco
        assertValor("20.00", reabrir().clientes().buscarPorId(1).orElseThrow().getContaFiado().getSaldoDevedor());
    }

    private void executar(String sql) throws Exception {
        try (var conexao = banco.abrir(); var comando = conexao.createStatement()) {
            comando.execute(sql);
        }
    }

    @Test
    @DisplayName("Os dados de exemplo inteiros sobrevivem a fechar e abrir o sistema")
    void dadosDeExemploVoltamIguais() {
        Repositorios repositorios = reabrir();
        DadosExemplo.carregar(new ClienteService(repositorios.clientes()), new ProdutoService(repositorios.produtos()),
                new PedidoService(repositorios.pedidos(), RELOGIO),
                new FiadoService(repositorios.clientes()), RELOGIO);

        Repositorios depois = reabrir();
        PedidoService pedidos = new PedidoService(depois.pedidos(), RELOGIO);
        FiadoService fiado = new FiadoService(depois.clientes());
        assertEquals(8, depois.produtos().listarTodos().size());
        assertEquals(6, depois.clientes().listarTodos().size());
        assertEquals(10, pedidos.listarTodos().size());
        assertEquals(1, pedidos.contarAtrasados());
        assertValor("485.00", fiado.totalAReceber());
        assertEquals(2, fiado.contarDevedores());
    }
}
