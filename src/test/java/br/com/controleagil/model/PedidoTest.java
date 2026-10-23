package br.com.controleagil.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static br.com.controleagil.model.ContaFiadoTest.assertValor;
import static br.com.controleagil.model.ContaFiadoTest.reais;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PedidoTest {

    private Cliente cliente;
    private Produto coxinha;
    private Produto pastel;
    private Pedido pedido;

    @BeforeEach
    void montarPedido() {
        cliente = new Cliente("Maria", "(98) 90000-0000", "Rua A, 10");
        coxinha = new Produto("Coxinha", reais("5.00"));
        pastel = new Produto("Pastel", reais("3.50"));
        pedido = new Pedido(cliente, LocalDateTime.now().plusHours(2));
    }

    @Test
    @DisplayName("Todo pedido nasce PENDENTE")
    void pedidoNovoComecaPendente() {
        assertEquals(StatusPedido.PENDENTE, pedido.getStatus());
    }

    @Test
    @DisplayName("Pedido sem data e hora de entrega é recusado")
    void pedidoSemEntregaERecusado() {
        assertThrows(IllegalArgumentException.class, () -> new Pedido(cliente, null));
    }

    @Test
    @DisplayName("O total soma os subtotais dos itens")
    void totalSomaOsItens() {
        pedido.adicionarItem(coxinha, 10);
        pedido.adicionarItem(pastel, 4);
        assertValor("64.00", pedido.calcularTotal());
    }

    @Test
    @DisplayName("O item guarda o preço do dia da venda")
    void itemGuardaOPrecoDaVenda() {
        pedido.adicionarItem(coxinha, 10);
        coxinha.alterarPreco(reais("6.00"));
        assertValor("5.00", pedido.getItens().get(0).getPrecoUnitario());
        assertValor("50.00", pedido.calcularTotal());
    }

    @Test
    @DisplayName("O status só anda para frente")
    void statusSoAndaParaFrente() {
        pedido.alterarStatus(StatusPedido.EM_PRODUCAO);
        assertThrows(IllegalStateException.class, () -> pedido.alterarStatus(StatusPedido.PENDENTE));

        pedido.alterarStatus(StatusPedido.ENTREGUE);
        assertThrows(IllegalStateException.class, () -> pedido.alterarStatus(StatusPedido.CANCELADO));
    }

    @Test
    @DisplayName("Dá para cancelar antes da entrega")
    void podeCancelarAntesDaEntrega() {
        pedido.alterarStatus(StatusPedido.SAIU_PARA_ENTREGA);
        pedido.alterarStatus(StatusPedido.CANCELADO);
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
    }

    @Test
    @DisplayName("Itens só mudam enquanto o pedido está PENDENTE")
    void itensSoMudamNoPendente() {
        pedido.adicionarItem(coxinha, 1);
        pedido.alterarStatus(StatusPedido.EM_PRODUCAO);
        assertThrows(IllegalStateException.class, () -> pedido.adicionarItem(pastel, 1));
    }

    @Test
    @DisplayName("Não dá para pagar mais do que falta")
    void pagamentoMaiorQueOTotalERecusado() {
        pedido.adicionarItem(coxinha, 2);
        assertThrows(IllegalArgumentException.class,
                () -> pedido.registrarPagamento(new PagamentoPix(reais("10.01"), "E123")));
    }

    @Test
    @DisplayName("Alerta de entrega próxima respeita a janela de minutos")
    void alertaDeEntregaProxima() {
        Pedido daquiAVinteMinutos = new Pedido(cliente, LocalDateTime.now().plusMinutes(20));
        assertTrue(daquiAVinteMinutos.estaProximoDaEntrega(30));
        assertFalse(daquiAVinteMinutos.estaProximoDaEntrega(10));

        daquiAVinteMinutos.alterarStatus(StatusPedido.ENTREGUE);
        assertFalse(daquiAVinteMinutos.estaProximoDaEntrega(30));
    }
}
