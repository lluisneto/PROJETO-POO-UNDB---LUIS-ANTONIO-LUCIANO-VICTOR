package br.com.controleagil.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Conta fiado de um cliente: substitui o caderno.
 *
 * Encapsulamento: não existe setSaldo(). O saldo devedor só muda por
 * registrarCompra() e registrarPagamento(), que validam as regras.
 */
public class ContaFiado {

    private final BigDecimal limiteCredito;
    private final List<MovimentacaoFiado> movimentacoes = new ArrayList<>();

    public ContaFiado(BigDecimal limiteCredito) {
        if (limiteCredito == null || limiteCredito.signum() < 0) {
            throw new IllegalArgumentException("O limite de crédito não pode ser negativo.");
        }
        this.limiteCredito = limiteCredito;
    }

    public void registrarCompra(BigDecimal valor, Pedido pedido) {
        validarValorPositivo(valor);
        if (!podeComprar(valor)) {
            throw new IllegalStateException("A compra de R$ " + valor
                    + " ultrapassa o crédito disponível de R$ " + getCreditoDisponivel() + ".");
        }
        movimentacoes.add(new MovimentacaoFiado(valor, TipoMovimentacao.COMPRA, pedido));
    }

    public void registrarPagamento(BigDecimal valor) {
        validarValorPositivo(valor);
        if (valor.compareTo(getSaldoDevedor()) > 0) {
            throw new IllegalArgumentException("O pagamento de R$ " + valor
                    + " é maior que o saldo devedor de R$ " + getSaldoDevedor() + ".");
        }
        movimentacoes.add(new MovimentacaoFiado(valor, TipoMovimentacao.PAGAMENTO, null));
    }

    /** O saldo não fica guardado: é sempre calculado a partir das movimentações. */
    public BigDecimal getSaldoDevedor() {
        BigDecimal saldo = BigDecimal.ZERO;
        for (MovimentacaoFiado movimentacao : movimentacoes) {
            if (movimentacao.getTipo() == TipoMovimentacao.COMPRA) {
                saldo = saldo.add(movimentacao.getValor());
            } else {
                saldo = saldo.subtract(movimentacao.getValor());
            }
        }
        return saldo;
    }

    public BigDecimal getCreditoDisponivel() {
        return limiteCredito.subtract(getSaldoDevedor());
    }

    public boolean podeComprar(BigDecimal valor) {
        return valor.compareTo(getCreditoDisponivel()) <= 0;
    }

    public BigDecimal getLimiteCredito() {
        return limiteCredito;
    }

    /** Lista somente leitura: ninguém de fora consegue adicionar ou apagar movimentações. */
    public List<MovimentacaoFiado> getMovimentacoes() {
        return Collections.unmodifiableList(movimentacoes);
    }

    private void validarValorPositivo(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor precisa ser maior que zero.");
        }
    }
}
