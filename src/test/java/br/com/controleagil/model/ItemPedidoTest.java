package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static br.com.controleagil.TesteDinheiro.assertValor;
import static br.com.controleagil.TesteDinheiro.reais;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 * Responsabilidade: testar o ItemPedido: subtotal, preço da época e quantidade mínima.
 */
class ItemPedidoTest {

    private final Produto coxinha = new Produto("Coxinha", reais("5.50"));

    @Test
    @DisplayName("Subtotal = preço unitário × quantidade")
    void subtotal() {
        ItemPedido item = new ItemPedido(coxinha, 3);
        assertValor("16.50", item.calcularSubtotal());
    }

    @Test
    @DisplayName("O item copia o preço do produto no momento em que é criado")
    void itemCopiaOPrecoDaEpoca() {
        ItemPedido item = new ItemPedido(coxinha, 2);
        coxinha.alterarPreco(reais("6.00"));

        assertValor("5.50", item.getPrecoUnitario());
        assertValor("11.00", item.calcularSubtotal());
    }

    @Test
    @DisplayName("Somar quantidade cria um item novo com o mesmo preço unitário")
    void somarQuantidadeMantemOPreco() {
        ItemPedido item = new ItemPedido(coxinha, 2);
        coxinha.alterarPreco(reais("9.00"));

        ItemPedido somado = item.somarQuantidade(3);

        assertEquals(5, somado.getQuantidade());
        assertValor("5.50", somado.getPrecoUnitario());
        assertEquals(2, item.getQuantidade(), "o item original não muda (é imutável)");
    }

    @Test
    @DisplayName("Quantidade menor que 1 é recusada")
    void quantidadeMenorQueUmLancaExcecao() {
        assertThrows(RegraNegocioException.class, () -> new ItemPedido(coxinha, 0));
        assertThrows(RegraNegocioException.class, () -> new ItemPedido(coxinha, -2));
        assertThrows(RegraNegocioException.class, () -> new ItemPedido(null, 1));
    }
}
