package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static br.com.controleagil.TesteDinheiro.assertValor;
import static br.com.controleagil.TesteDinheiro.reais;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Responsabilidade: testar as formas de pagamento (dinheiro, Pix, cartão e fiado).
 * Conceito mostrado: POLIMORFISMO. O Pedido chama processar() e cada forma age do seu jeito.
 */
class PagamentoTest {

    private Cliente cliente;
    private Pedido pedido;

    @BeforeEach
    void montarPedidoDe64Reais() {
        cliente = new Cliente("João", "(98) 90000-0001", "Rua B, 25");
        pedido = new Pedido(cliente, LocalDateTime.now().plusHours(1));
        pedido.adicionarItem(new Produto("Coxinha", reais("5.00")), 10);
        pedido.adicionarItem(new Produto("Kibe", reais("3.50")), 4);
    }

    @Test
    @DisplayName("Dinheiro: calcula o troco")
    void trocoDoPagamentoDinheiro() {
        PagamentoDinheiro dinheiro = new PagamentoDinheiro(reais("64.00"), reais("70.00"));
        pedido.registrarPagamento(dinheiro);

        assertValor("6.00", dinheiro.calcularTroco());
        assertValor("64.00", pedido.calcularValorPago());
    }

    @Test
    @DisplayName("Dinheiro: valor recebido menor que o valor a pagar é recusado")
    void dinheiroInsuficienteLancaExcecao() {
        assertThrows(RegraNegocioException.class,
                () -> pedido.registrarPagamento(new PagamentoDinheiro(reais("64.00"), reais("50.00"))));
        assertTrue(pedido.getPagamentos().isEmpty());
    }

    @Test
    @DisplayName("Pix: o código é opcional, mas se vier precisa ter formato válido")
    void pixCodigoOpcional() {
        PagamentoPix semCodigo = new PagamentoPix(reais("10.00"), "   ");
        pedido.registrarPagamento(semCodigo);
        assertNull(semCodigo.getCodigoTransacao());

        assertThrows(RegraNegocioException.class,
                () -> pedido.registrarPagamento(new PagamentoPix(reais("10.00"), "código@inválido!")));
        pedido.registrarPagamento(new PagamentoPix(reais("10.00"), "E2E 1234 ABCD"));
        assertValor("20.00", pedido.calcularValorPago());
    }

    @Test
    @DisplayName("Fiado: lança a compra na conta do cliente, ligada ao pedido")
    void pagamentoFiadoLancaCompraNaConta() {
        cliente.abrirContaFiado(reais("100.00"));
        pedido.registrarPagamento(new PagamentoFiado(reais("64.00")));

        ContaFiado conta = cliente.getContaFiado();
        assertValor("64.00", conta.getSaldoDevedor());
        assertEquals(TipoMovimentacao.COMPRA, conta.getMovimentacoes().get(0).getTipo());
        assertSame(pedido, conta.getMovimentacoes().get(0).getPedido());
    }

    @Test
    @DisplayName("Fiado: cliente sem conta fiado lança exceção")
    void pagamentoFiadoSemContaLancaExcecao() {
        assertThrows(RegraNegocioException.class,
                () -> pedido.registrarPagamento(new PagamentoFiado(reais("64.00"))));
        assertTrue(pedido.getPagamentos().isEmpty());
    }

    @Test
    @DisplayName("Fiado: compra acima do limite é aceita (RN03 só avisa) e a dívida passa do limite")
    void pagamentoFiadoAcimaDoLimiteEhAceito() {
        cliente.abrirContaFiado(reais("50.00"));
        pedido.registrarPagamento(new PagamentoFiado(reais("64.00")));

        assertValor("64.00", pedido.calcularValorPago());
        assertValor("64.00", cliente.getContaFiado().getSaldoDevedor());
        assertValor("14.00", cliente.getContaFiado().getValorAcimaDoLimite());
    }

    @Test
    @DisplayName("Cartão: débito e crédito entram no pedido com a descrição certa")
    void pagamentoNoCartao() {
        PagamentoCartao debito = new PagamentoCartao(reais("40.00"), TipoCartao.DEBITO);
        PagamentoCartao credito = new PagamentoCartao(reais("24.00"), TipoCartao.CREDITO);
        pedido.registrarPagamento(debito);
        pedido.registrarPagamento(credito);

        assertValor("64.00", pedido.calcularValorPago());
        assertEquals("Cartão de débito", debito.getDescricao());
        assertEquals("Cartão de crédito", credito.getDescricao());
    }

    @Test
    @DisplayName("Cartão: sem dizer se é débito ou crédito, o pagamento é recusado")
    void cartaoSemTipoLancaExcecao() {
        assertThrows(RegraNegocioException.class,
                () -> pedido.registrarPagamento(new PagamentoCartao(reais("64.00"), null)));
        assertTrue(pedido.getPagamentos().isEmpty());
    }

    @Test
    @DisplayName("Cancelar pedido pago no cartão não mexe no fiado")
    void cancelarPedidoNoCartaoNaoMexeNoFiado() {
        cliente.abrirContaFiado(reais("100.00"));
        pedido.registrarPagamento(new PagamentoCartao(reais("64.00"), TipoCartao.CREDITO));
        pedido.alterarStatus(StatusPedido.CANCELADO);
        assertTrue(cliente.getContaFiado().getMovimentacoes().isEmpty());
    }

    @Test
    @DisplayName("Cancelar pedido pago no fiado estorna a compra: a dívida volta ao que era")
    void cancelarPedidoFiadoEstornaACompra() {
        cliente.abrirContaFiado(reais("100.00"));
        pedido.registrarPagamento(new PagamentoFiado(reais("64.00")));

        pedido.alterarStatus(StatusPedido.CANCELADO);

        ContaFiado conta = cliente.getContaFiado();
        assertValor("0", conta.getSaldoDevedor());
        assertEquals(TipoMovimentacao.ESTORNO, conta.getMovimentacoes().get(1).getTipo());
        assertSame(pedido, conta.getMovimentacoes().get(1).getPedido());
    }

    @Test
    @DisplayName("Estorno não passa da dívida: o que o cliente já pagou é devolvido por fora")
    void estornoLimitadoAoSaldo() {
        cliente.abrirContaFiado(reais("100.00"));
        pedido.registrarPagamento(new PagamentoFiado(reais("64.00")));
        cliente.getContaFiado().registrarPagamento(reais("50.00")); // deve 14

        pedido.alterarStatus(StatusPedido.CANCELADO);

        assertValor("0", cliente.getContaFiado().getSaldoDevedor());
        assertValor("14.00", cliente.getContaFiado().getMovimentacoes().get(2).getValor());
    }

    @Test
    @DisplayName("Cancelar pedido pago em dinheiro não mexe no fiado")
    void cancelarPedidoEmDinheiroNaoMexeNoFiado() {
        cliente.abrirContaFiado(reais("100.00"));
        pedido.registrarPagamento(new PagamentoDinheiro(reais("64.00"), reais("64.00")));
        pedido.alterarStatus(StatusPedido.CANCELADO);
        assertTrue(cliente.getContaFiado().getMovimentacoes().isEmpty());
    }

    @Test
    @DisplayName("Dá para dividir o pagamento em formas diferentes")
    void pagamentoDividido() {
        cliente.abrirContaFiado(reais("100.00"));
        pedido.registrarPagamento(new PagamentoPix(reais("40.00"), "E2E0001"));
        pedido.registrarPagamento(new PagamentoFiado(reais("24.00")));

        assertValor("64.00", pedido.calcularValorPago());
        assertValor("24.00", cliente.getContaFiado().getSaldoDevedor());
    }

    @Test
    @DisplayName("Valor do pagamento precisa ser maior que zero")
    void valorZeroLancaExcecao() {
        assertThrows(RegraNegocioException.class, () -> new PagamentoFiado(reais("0")));
        // 0,004 é maior que zero, mas com 2 casas vira 0,00: também é recusado
        assertThrows(RegraNegocioException.class, () -> new PagamentoPix(reais("0.004"), null));
        assertThrows(RegraNegocioException.class, () -> new Produto("Coxinha", reais("0.004")));
        assertThrows(RegraNegocioException.class, () -> new PagamentoDinheiro(reais("10.00"), null));
    }
}
