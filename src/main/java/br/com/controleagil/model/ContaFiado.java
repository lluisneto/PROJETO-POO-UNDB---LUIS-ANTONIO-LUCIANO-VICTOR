package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Responsabilidade: a "página do caderno" de um cliente: o limite de crédito e o histórico
 * de compras e pagamentos no fiado.
 *
 * Conceito de POO: ENCAPSULAMENTO, o exemplo principal do projeto.
 *  - Não existe setSaldo(). O saldo devedor nem é guardado: é CALCULADO a partir do histórico
 *    (getSaldoDevedor). Assim o saldo nunca fica diferente do que está no caderno.
 *  - O histórico só cresce por registrarCompra() e registrarPagamento(), que validam as regras
 *    RN03 e RN04 ANTES de anotar qualquer coisa.
 *  - getMovimentacoes() devolve uma lista que não pode ser alterada de fora.
 */
public class ContaFiado {

    private final BigDecimal limiteCredito;
    private final List<MovimentacaoFiado> movimentacoes = new ArrayList<>();

    /** @param limiteCredito quanto o cliente pode ficar devendo; zero ou mais */
    public ContaFiado(BigDecimal limiteCredito) {
        if (limiteCredito == null || limiteCredito.signum() < 0) {
            throw new RegraNegocioException("O limite de crédito não pode ser negativo.");
        }
        this.limiteCredito = Dinheiro.arredondar(limiteCredito);
    }

    /**
     * Anota uma compra fiado (a dívida aumenta).
     *
     * @param valor  valor da compra, maior que zero
     * @param pedido pedido que originou a compra (pode ser null)
     */
    public void registrarCompra(BigDecimal valor, Pedido pedido) {
        BigDecimal compra = exigirValorPositivo(valor, "O valor da compra precisa ser maior que zero.");
        // RN03: compra fiado não pode passar do limite de crédito. Neste esqueleto, BLOQUEIA a compra.
        if (!podeComprar(compra)) {
            throw new RegraNegocioException("Compra fiado de " + Dinheiro.formatar(compra)
                    + " passa do limite. Crédito disponível: " + Dinheiro.formatar(getCreditoDisponivel())
                    + " (limite de " + Dinheiro.formatar(limiteCredito) + ").");
        }
        movimentacoes.add(new MovimentacaoFiado(compra, TipoMovimentacao.COMPRA, pedido));
    }

    /**
     * Anota um pagamento da dívida (a dívida diminui). Pode ser total ou parcial.
     *
     * @param valor valor pago, maior que zero e no máximo o saldo devedor
     */
    public void registrarPagamento(BigDecimal valor) {
        // RN04: o pagamento precisa ser maior que zero...
        BigDecimal pagamento = exigirValorPositivo(valor, "O valor do pagamento precisa ser maior que zero.");
        BigDecimal saldo = getSaldoDevedor();
        if (saldo.signum() == 0) {
            throw new RegraNegocioException("Este cliente não está devendo nada no fiado.");
        }
        // ...e não pode ser maior que o saldo devedor.
        if (pagamento.compareTo(saldo) > 0) {
            throw new RegraNegocioException("O pagamento de " + Dinheiro.formatar(pagamento)
                    + " é maior que o saldo devedor de " + Dinheiro.formatar(saldo) + ".");
        }
        movimentacoes.add(new MovimentacaoFiado(pagamento, TipoMovimentacao.PAGAMENTO, null));
    }

    /**
     * Quanto o cliente deve agora: soma das compras menos soma dos pagamentos.
     * É o atributo derivado "/saldoDevedor" do modelo: calculado, nunca guardado.
     */
    public BigDecimal getSaldoDevedor() {
        BigDecimal saldo = Dinheiro.ZERO;
        for (MovimentacaoFiado movimentacao : movimentacoes) {
            if (movimentacao.getTipo() == TipoMovimentacao.COMPRA) {
                saldo = saldo.add(movimentacao.getValor());
            } else {
                saldo = saldo.subtract(movimentacao.getValor());
            }
        }
        return saldo;
    }

    /** Quanto ainda dá para comprar fiado: limite − saldo devedor. */
    public BigDecimal getCreditoDisponivel() {
        return limiteCredito.subtract(getSaldoDevedor());
    }

    /** Diz se uma compra deste valor cabe no crédito disponível (usado pela RN03). */
    public boolean podeComprar(BigDecimal valor) {
        // compareTo, e não equals: para o BigDecimal, 5.0 e 5.00 são "diferentes" no equals.
        return valor != null && valor.compareTo(getCreditoDisponivel()) <= 0;
    }

    /** Quanto o cliente pode ficar devendo no máximo. */
    public BigDecimal getLimiteCredito() {
        return limiteCredito;
    }

    /** O histórico, do mais antigo para o mais novo. A lista é somente leitura. */
    public List<MovimentacaoFiado> getMovimentacoes() {
        return Collections.unmodifiableList(movimentacoes);
    }

    /** Arredonda para 2 casas e só depois confere se é maior que zero (0,004 vira 0,00 e é recusado). */
    private static BigDecimal exigirValorPositivo(BigDecimal valor, String mensagem) {
        return Dinheiro.positivo(valor, mensagem);
    }
}
