package br.com.controleagil.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContaFiadoTest {

    private ContaFiado conta;

    @BeforeEach
    void criarConta() {
        conta = new ContaFiado(reais("100.00"));
    }

    @Test
    @DisplayName("Uma conta nova não deve nada")
    void contaNovaComecaZerada() {
        assertValor("0", conta.getSaldoDevedor());
        assertValor("100.00", conta.getCreditoDisponivel());
    }

    @Test
    @DisplayName("Compras somam e pagamentos subtraem do saldo")
    void comprasEPagamentosAtualizamOSaldo() {
        conta.registrarCompra(reais("50.00"), null);
        conta.registrarPagamento(reais("20.00"));
        conta.registrarCompra(reais("30.00"), null);

        assertValor("60.00", conta.getSaldoDevedor());
        assertValor("40.00", conta.getCreditoDisponivel());
        assertEquals(3, conta.getMovimentacoes().size());
    }

    @Test
    @DisplayName("Compra acima do crédito disponível é recusada e o saldo não muda")
    void compraAcimaDoLimiteERecusada() {
        conta.registrarCompra(reais("60.00"), null);

        assertFalse(conta.podeComprar(reais("60.00")));
        assertThrows(IllegalStateException.class, () -> conta.registrarCompra(reais("60.00"), null));
        assertValor("60.00", conta.getSaldoDevedor());
    }

    @Test
    @DisplayName("Dá para comprar exatamente o crédito que sobra")
    void compraNoLimiteExatoEAceita() {
        assertTrue(conta.podeComprar(reais("100.00")));
        conta.registrarCompra(reais("100.00"), null);
        assertValor("0.00", conta.getCreditoDisponivel());
    }

    @Test
    @DisplayName("Pagamento maior que a dívida é recusado")
    void pagamentoMaiorQueADividaERecusado() {
        conta.registrarCompra(reais("30.00"), null);
        assertThrows(IllegalArgumentException.class, () -> conta.registrarPagamento(reais("30.01")));
    }

    @Test
    @DisplayName("Valores zerados ou negativos são recusados")
    void valoresInvalidosSaoRecusados() {
        assertThrows(IllegalArgumentException.class, () -> conta.registrarCompra(BigDecimal.ZERO, null));
        assertThrows(IllegalArgumentException.class, () -> conta.registrarCompra(reais("-5.00"), null));
        assertThrows(IllegalArgumentException.class, () -> conta.registrarPagamento(null));
        assertThrows(IllegalArgumentException.class, () -> new ContaFiado(reais("-1.00")));
    }

    @Test
    @DisplayName("O histórico não pode ser alterado de fora da conta (encapsulamento)")
    void historicoEhSomenteLeitura() {
        conta.registrarCompra(reais("10.00"), null);
        assertThrows(UnsupportedOperationException.class, () -> conta.getMovimentacoes().clear());
        assertEquals(TipoMovimentacao.COMPRA, conta.getMovimentacoes().get(0).getTipo());
    }

    static BigDecimal reais(String valor) {
        return new BigDecimal(valor);
    }

    static void assertValor(String esperado, BigDecimal atual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(atual), "esperado R$ " + esperado + ", veio R$ " + atual);
    }
}
