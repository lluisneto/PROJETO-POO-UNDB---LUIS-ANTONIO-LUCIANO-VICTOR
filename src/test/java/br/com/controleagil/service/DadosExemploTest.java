package br.com.controleagil.service;

import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.Pedido;
import br.com.controleagil.model.StatusPedido;
import br.com.controleagil.repository.memoria.ClienteRepositoryMemoria;
import br.com.controleagil.repository.memoria.PedidoRepositoryMemoria;
import br.com.controleagil.repository.memoria.ProdutoRepositoryMemoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static br.com.controleagil.TesteDinheiro.assertValor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Responsabilidade: garantir que os dados de exemplo carregam sem quebrar nenhuma regra,
 * a QUALQUER hora do dia, e que sempre deixam a agenda com os destaques esperados.
 */
class DadosExemploTest {

    private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");

    private ClienteService clientes;
    private ProdutoService produtos;
    private PedidoService pedidos;
    private FiadoService fiado;
    private LocalDateTime agora;

    @ParameterizedTest(name = "abrindo o sistema às {0}")
    @ValueSource(strings = {"00:15", "05:50", "06:10", "10:00", "15:45", "20:45", "21:50", "23:30", "23:55"})
    @DisplayName("A qualquer hora: 1 atrasado, 1 em breve e os entregues no passado")
    void carregaAQualquerHora(String hora) {
        carregarAs(LocalTime.parse(hora));

        assertEquals(8, produtos.listarTodos().size());
        assertEquals(6, clientes.listarTodos().size());
        assertEquals(10, pedidos.listarTodos().size());
        assertEquals(3, fiado.listarContas(false).size());

        assertEquals(1, pedidos.contarAtrasados(), "um pedido atrasado");
        assertEquals(1, pedidos.listarTodos().stream().filter(pedidos::estaEmBreve).count(), "um pedido em breve");
        for (Pedido pedido : pedidos.listarTodos()) {
            if (pedido.getStatus() == StatusPedido.ENTREGUE) {
                assertTrue(pedido.getDataHoraEntrega().isBefore(agora), "pedido entregue não pode estar no futuro");
            }
        }
        assertValor("485.00", fiado.totalAReceber());
    }

    @Test
    @DisplayName("Às 10:00 tudo fica no dia de hoje e o fiado está nas três situações")
    void meioDaManha() {
        carregarAs(LocalTime.of(10, 0));

        assertEquals(8, pedidos.pedidosDoDia(pedidos.hoje(), null).size());
        assertEquals(7, pedidos.contarPedidosDeHoje(), "o cancelado não conta");

        Cliente mercadinho = clientes.buscar("Mercadinho").get(0);
        assertValor("45.00", mercadinho.getContaFiado().getCreditoDisponivel()); // perto do limite
        Cliente lanchonete = clientes.buscar("Lanchonete").get(0);
        assertValor("30.00", lanchonete.getContaFiado().getSaldoDevedor()); // saldo baixo
        Cliente cantina = clientes.buscar("Cantina").get(0);
        assertValor("0", cantina.getContaFiado().getSaldoDevedor()); // sem dívida
        assertEquals(2, cantina.getContaFiado().getMovimentacoes().size());
    }

    private void carregarAs(LocalTime hora) {
        agora = LocalDateTime.of(2026, 10, 29, hora.getHour(), hora.getMinute());
        Clock relogio = Clock.fixed(agora.atZone(FUSO).toInstant(), FUSO);

        ClienteRepositoryMemoria repositorioClientes = new ClienteRepositoryMemoria();
        clientes = new ClienteService(repositorioClientes);
        produtos = new ProdutoService(new ProdutoRepositoryMemoria());
        pedidos = new PedidoService(new PedidoRepositoryMemoria(), relogio);
        fiado = new FiadoService(repositorioClientes);

        DadosExemplo.carregar(clientes, produtos, pedidos, fiado, relogio);
    }
}
