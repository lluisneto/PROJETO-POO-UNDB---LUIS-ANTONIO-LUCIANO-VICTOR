package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static br.com.controleagil.TesteDinheiro.assertValor;
import static br.com.controleagil.TesteDinheiro.reais;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Responsabilidade: testar as regras do Pedido: total, preço da época, RN01, RN05,
 * ordem dos status e limite dos pagamentos.
 */
class PedidoTest {

    private Cliente cliente;
    private Produto coxinha;
    private Produto kibe;
    private Pedido pedido;

    @BeforeEach
    void montarPedido() {
        cliente = new Cliente("Maria", "(98) 90000-0000", "Rua A, 10");
        coxinha = new Produto("Coxinha", reais("5.00"));
        kibe = new Produto("Kibe", reais("3.50"));
        pedido = new Pedido(cliente, LocalDateTime.now().plusHours(2));
    }

    @Test
    @DisplayName("Todo pedido nasce pendente")
    void pedidoNovoComecaPendente() {
        assertEquals(StatusPedido.PENDENTE, pedido.getStatus());
    }

    @Test
    @DisplayName("RN01: pedido sem data e hora de entrega lança exceção")
    void pedidoSemDataHoraLancaExcecao() {
        assertThrows(RegraNegocioException.class, () -> new Pedido(cliente, null));
    }

    @Test
    @DisplayName("O total soma os subtotais de vários itens")
    void totalComVariosItens() {
        pedido.adicionarItem(coxinha, 10);
        pedido.adicionarItem(kibe, 4);
        pedido.adicionarItem(new Produto("Empada", reais("4.25")), 2);

        assertValor("72.50", pedido.calcularTotal()); // 50,00 + 14,00 + 8,50
    }

    @Test
    @DisplayName("Adicionar de novo um produto que já está no pedido soma a quantidade")
    void mesmoProdutoSomaQuantidade() {
        pedido.adicionarItem(coxinha, 3);
        pedido.adicionarItem(coxinha, 2);

        assertEquals(1, pedido.getItens().size());
        assertEquals(5, pedido.getItens().get(0).getQuantidade());
    }

    @Test
    @DisplayName("O preço da época é mantido depois de alterarPreco no produto")
    void precoDaEpocaMantidoAposAlterarPreco() {
        pedido.adicionarItem(coxinha, 10);
        coxinha.alterarPreco(reais("6.00"));

        assertValor("5.00", pedido.getItens().get(0).getPrecoUnitario());
        assertValor("50.00", pedido.calcularTotal());
    }

    @Test
    @DisplayName("O status avança em ordem, um passo por vez")
    void avancoDeStatusEmOrdem() {
        pedido.alterarStatus(StatusPedido.EM_PRODUCAO);
        pedido.alterarStatus(StatusPedido.SAIU_PARA_ENTREGA);
        pedido.alterarStatus(StatusPedido.ENTREGUE);
        assertEquals(StatusPedido.ENTREGUE, pedido.getStatus());
    }

    @Test
    @DisplayName("Não dá para pular etapas nem voltar o status")
    void statusNaoPulaNemVolta() {
        assertThrows(RegraNegocioException.class, () -> pedido.alterarStatus(StatusPedido.SAIU_PARA_ENTREGA));
        assertThrows(RegraNegocioException.class, () -> pedido.alterarStatus(StatusPedido.ENTREGUE));

        pedido.alterarStatus(StatusPedido.EM_PRODUCAO);
        assertThrows(RegraNegocioException.class, () -> pedido.alterarStatus(StatusPedido.PENDENTE));
        assertEquals(StatusPedido.EM_PRODUCAO, pedido.getStatus());
    }

    @Test
    @DisplayName("StatusPedido.proximo() segue a ordem e não existe para status final")
    void proximoStatus() {
        assertEquals(StatusPedido.EM_PRODUCAO, StatusPedido.PENDENTE.proximo());
        assertEquals(StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.EM_PRODUCAO.proximo());
        assertEquals(StatusPedido.ENTREGUE, StatusPedido.SAIU_PARA_ENTREGA.proximo());
        assertThrows(RegraNegocioException.class, StatusPedido.ENTREGUE::proximo);
        assertThrows(RegraNegocioException.class, StatusPedido.CANCELADO::proximo);
    }

    @Test
    @DisplayName("Dá para cancelar de qualquer status que ainda não seja final")
    void podeCancelarAntesDoFim() {
        pedido.alterarStatus(StatusPedido.EM_PRODUCAO);
        pedido.alterarStatus(StatusPedido.SAIU_PARA_ENTREGA);
        pedido.alterarStatus(StatusPedido.CANCELADO);
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
    }

    @Test
    @DisplayName("RN05: pedido entregue ou cancelado não muda mais de status")
    void statusFinalNaoMuda() {
        pedido.alterarStatus(StatusPedido.EM_PRODUCAO);
        pedido.alterarStatus(StatusPedido.SAIU_PARA_ENTREGA);
        pedido.alterarStatus(StatusPedido.ENTREGUE);
        assertThrows(RegraNegocioException.class, () -> pedido.alterarStatus(StatusPedido.CANCELADO));

        Pedido cancelado = new Pedido(cliente, LocalDateTime.now().plusHours(1));
        cancelado.alterarStatus(StatusPedido.CANCELADO);
        assertThrows(RegraNegocioException.class, () -> cancelado.alterarStatus(StatusPedido.EM_PRODUCAO));
        assertEquals(StatusPedido.CANCELADO, cancelado.getStatus());
    }

    @Test
    @DisplayName("Itens só mudam enquanto o pedido está pendente")
    void itensSoMudamNoPendente() {
        pedido.adicionarItem(coxinha, 1);
        pedido.alterarStatus(StatusPedido.EM_PRODUCAO);
        assertThrows(RegraNegocioException.class, () -> pedido.adicionarItem(kibe, 1));
    }

    @Test
    @DisplayName("A soma dos pagamentos não passa do total")
    void somaDosPagamentosNaoPassaDoTotal() {
        pedido.adicionarItem(coxinha, 2); // total R$ 10,00
        pedido.registrarPagamento(new PagamentoPix(reais("6.00"), null));

        assertThrows(RegraNegocioException.class,
                () -> pedido.registrarPagamento(new PagamentoPix(reais("4.01"), null)));
        pedido.registrarPagamento(new PagamentoPix(reais("4.00"), null));
        assertValor("10.00", pedido.calcularValorPago());
    }

    @Test
    @DisplayName("Pedido cancelado não recebe pagamento")
    void pedidoCanceladoNaoRecebePagamento() {
        pedido.adicionarItem(coxinha, 2);
        pedido.alterarStatus(StatusPedido.CANCELADO);
        assertThrows(RegraNegocioException.class,
                () -> pedido.registrarPagamento(new PagamentoPix(reais("10.00"), null)));
    }

    @Test
    @DisplayName("Entrega próxima respeita a janela de minutos e ignora pedido finalizado")
    void entregaProxima() {
        LocalDateTime agora = LocalDateTime.of(2026, 10, 29, 10, 0);
        Pedido asDezEVinte = new Pedido(cliente, agora.plusMinutes(20));

        assertTrue(asDezEVinte.estaProximoDaEntrega(30, agora));
        assertFalse(asDezEVinte.estaProximoDaEntrega(10, agora));
        assertTrue(asDezEVinte.estaProximoDaEntrega(10, agora.plusHours(1)), "atrasado também conta");

        asDezEVinte.alterarStatus(StatusPedido.CANCELADO);
        assertFalse(asDezEVinte.estaProximoDaEntrega(30, agora));
    }
}
