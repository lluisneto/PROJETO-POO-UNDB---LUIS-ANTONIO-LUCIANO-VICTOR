package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static br.com.controleagil.TesteDinheiro.assertValor;
import static br.com.controleagil.TesteDinheiro.reais;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Responsabilidade: testar as regras da ContaFiado (RN03 e RN04) e o saldo calculado.
 */
class ContaFiadoTest {

    private ContaFiado conta;

    @BeforeEach
    void criarContaComLimiteDe100() {
        conta = new ContaFiado(reais("100.00"));
    }

    @Test
    @DisplayName("Uma conta nova não deve nada e tem todo o limite disponível")
    void contaNovaComecaZerada() {
        assertValor("0", conta.getSaldoDevedor());
        assertValor("100.00", conta.getCreditoDisponivel());
    }

    @Test
    @DisplayName("Compra dentro do limite é aceita e entra no histórico")
    void compraDentroDoLimiteEhAceita() {
        conta.registrarCompra(reais("60.00"), null);

        assertValor("60.00", conta.getSaldoDevedor());
        assertEquals(1, conta.getMovimentacoes().size());
        assertEquals(TipoMovimentacao.COMPRA, conta.getMovimentacoes().get(0).getTipo());
    }

    @Test
    @DisplayName("Dá para comprar exatamente o crédito que sobra")
    void compraNoLimiteExatoEhAceita() {
        assertTrue(conta.podeComprar(reais("100.00")));
        conta.registrarCompra(reais("100.00"), null);
        assertValor("0.00", conta.getCreditoDisponivel());
    }

    @Test
    @DisplayName("RN03: compra acima do limite só gera aviso; ela entra na conta e a dívida fica acima do limite")
    void compraAcimaDoLimiteEntraNaConta() {
        conta.registrarCompra(reais("60.00"), null);

        assertFalse(conta.podeComprar(reais("40.01")), "a tela usa isto para avisar");
        assertValor("0.01", conta.quantoPassariaDoLimite(reais("40.01")));
        conta.registrarCompra(reais("40.01"), null);

        assertValor("100.01", conta.getSaldoDevedor());
        assertTrue(conta.estaAcimaDoLimite());
        assertValor("0.01", conta.getValorAcimaDoLimite());
        assertValor("0.00", conta.getCreditoDisponivel(), "o crédito disponível nunca fica negativo");
    }

    @Test
    @DisplayName("Dentro do limite, a conta não está acima do limite e nada passaria")
    void dentroDoLimiteNaoPassa() {
        conta.registrarCompra(reais("60.00"), null);
        assertFalse(conta.estaAcimaDoLimite());
        assertValor("0.00", conta.getValorAcimaDoLimite());
        assertValor("0.00", conta.quantoPassariaDoLimite(reais("40.00")));
    }

    @Test
    @DisplayName("RN04: pagamento maior que o saldo lança exceção")
    void pagamentoMaiorQueOSaldoLancaExcecao() {
        conta.registrarCompra(reais("30.00"), null);
        assertThrows(RegraNegocioException.class, () -> conta.registrarPagamento(reais("30.01")));
        assertValor("30.00", conta.getSaldoDevedor());
    }

    @Test
    @DisplayName("RN04: pagamento zero, negativo ou sem dívida lança exceção")
    void pagamentoZeroOuNegativoLancaExcecao() {
        assertThrows(RegraNegocioException.class, () -> conta.registrarPagamento(reais("10.00")));

        conta.registrarCompra(reais("30.00"), null);
        assertThrows(RegraNegocioException.class, () -> conta.registrarPagamento(BigDecimal.ZERO));
        assertThrows(RegraNegocioException.class, () -> conta.registrarPagamento(reais("-5.00")));
        assertThrows(RegraNegocioException.class, () -> conta.registrarPagamento(null));
    }

    @Test
    @DisplayName("O saldo é calculado a partir das movimentações: compras somam, pagamentos subtraem")
    void saldoCalculadoPelasMovimentacoes() {
        conta.registrarCompra(reais("50.00"), null);
        conta.registrarPagamento(reais("20.00"));
        conta.registrarCompra(reais("30.00"), null);

        assertValor("60.00", conta.getSaldoDevedor());
        assertEquals(3, conta.getMovimentacoes().size());
        assertEquals(TipoMovimentacao.PAGAMENTO, conta.getMovimentacoes().get(1).getTipo());
    }

    @Test
    @DisplayName("Crédito disponível = limite − saldo devedor")
    void creditoDisponivel() {
        conta.registrarCompra(reais("75.50"), null);
        assertValor("24.50", conta.getCreditoDisponivel());

        conta.registrarPagamento(reais("50.00"));
        assertValor("74.50", conta.getCreditoDisponivel());
    }

    @Test
    @DisplayName("Valores ficam com 2 casas decimais (arredondamento HALF_EVEN)")
    void valoresComDuasCasas() {
        conta.registrarCompra(reais("10.005"), null); // meio exato: vai para o par, 10.00
        assertEquals(new BigDecimal("10.00"), conta.getSaldoDevedor());
        assertEquals(2, conta.getLimiteCredito().scale());
    }

    @Test
    @DisplayName("Limite negativo e compra de valor zero são recusados")
    void valoresInvalidosSaoRecusados() {
        assertThrows(RegraNegocioException.class, () -> new ContaFiado(reais("-1.00")));
        assertThrows(RegraNegocioException.class, () -> conta.registrarCompra(BigDecimal.ZERO, null));
    }

    @Test
    @DisplayName("Alterar o limite muda o crédito; limite abaixo da dívida deixa a conta acima do limite")
    void alterarLimite() {
        conta.registrarCompra(reais("80.00"), null);

        conta.alterarLimite(reais("200.00"));
        assertValor("120.00", conta.getCreditoDisponivel());

        conta.alterarLimite(reais("50.00"));
        assertFalse(conta.podeComprar(reais("0.01")));
        assertValor("30.00", conta.getValorAcimaDoLimite());
        assertValor("80.00", conta.getSaldoDevedor(), "o histórico não muda");
        assertThrows(RegraNegocioException.class, () -> conta.alterarLimite(reais("-1")));
    }

    @Test
    @DisplayName("O histórico não pode ser alterado de fora da conta (encapsulamento)")
    void historicoEhSomenteLeitura() {
        conta.registrarCompra(reais("10.00"), null);
        assertThrows(UnsupportedOperationException.class, () -> conta.getMovimentacoes().clear());
    }
}
