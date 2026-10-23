package br.com.controleagil.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static br.com.controleagil.model.ContaFiadoTest.assertValor;
import static br.com.controleagil.model.ContaFiadoTest.reais;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Polimorfismo: o Pedido chama processar() e cada forma de pagamento age do seu jeito. */
class PagamentoTest {

    private Cliente cliente;
    private Pedido pedido;

    @BeforeEach
    void montarPedidoDe64Reais() {
        cliente = new Cliente("João", "(98) 90000-0001", "Rua B, 25");
        pedido = new Pedido(cliente, LocalDateTime.now().plusHours(1));
        pedido.adicionarItem(new Produto("Coxinha", reais("5.00")), 10);
        pedido.adicionarItem(new Produto("Pastel", reais("3.50")), 4);
    }

    @Test
    @DisplayName("Dinheiro: calcula o troco")
    void dinheiroCalculaTroco() {
        PagamentoDinheiro dinheiro = new PagamentoDinheiro(reais("64.00"), reais("70.00"));
        pedido.registrarPagamento(dinheiro);
        assertValor("6.00", dinheiro.calcularTroco());
        assertValor("64.00", pedido.calcularValorPago());
    }

    @Test
    @DisplayName("Dinheiro: valor recebido menor que o pagamento é recusado")
    void dinheiroInsuficienteERecusado() {
        assertThrows(IllegalStateException.class,
                () -> pedido.registrarPagamento(new PagamentoDinheiro(reais("64.00"), reais("50.00"))));
        assertTrue(pedido.getPagamentos().isEmpty());
    }

    @Test
    @DisplayName("Pix: sem o código da transação é recusado")
    void pixSemCodigoERecusado() {
        assertThrows(IllegalStateException.class,
                () -> pedido.registrarPagamento(new PagamentoPix(reais("64.00"), " ")));
    }

    @Test
    @DisplayName("Fiado: lança a compra na conta do cliente, ligada ao pedido")
    void fiadoLancaNaConta() {
        cliente.abrirContaFiado(reais("100.00"));
        pedido.registrarPagamento(new PagamentoFiado(reais("64.00")));

        ContaFiado conta = cliente.getContaFiado();
        assertValor("64.00", conta.getSaldoDevedor());
        assertEquals(TipoMovimentacao.COMPRA, conta.getMovimentacoes().get(0).getTipo());
        assertSame(pedido, conta.getMovimentacoes().get(0).getPedido());
    }

    @Test
    @DisplayName("Fiado: cliente sem conta fiado é recusado")
    void fiadoSemContaERecusado() {
        assertThrows(IllegalStateException.class,
                () -> pedido.registrarPagamento(new PagamentoFiado(reais("64.00"))));
    }

    @Test
    @DisplayName("Fiado: compra acima do limite é recusada e o pedido continua sem pagamento")
    void fiadoAcimaDoLimiteERecusado() {
        cliente.abrirContaFiado(reais("50.00"));
        assertThrows(IllegalStateException.class,
                () -> pedido.registrarPagamento(new PagamentoFiado(reais("64.00"))));
        assertValor("0", pedido.calcularValorPago());
    }

    @Test
    @DisplayName("Dá para dividir o pagamento em formas diferentes")
    void pagamentoDividido() {
        cliente.abrirContaFiado(reais("100.00"));
        pedido.registrarPagamento(new PagamentoPix(reais("40.00"), "E2E-0001"));
        pedido.registrarPagamento(new PagamentoFiado(reais("24.00")));
        assertValor("64.00", pedido.calcularValorPago());
        assertValor("24.00", cliente.getContaFiado().getSaldoDevedor());
    }
}
