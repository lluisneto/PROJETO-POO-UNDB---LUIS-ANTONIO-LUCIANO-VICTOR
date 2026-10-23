package br.com.controleagil.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Uma linha do "caderno": uma compra ou um pagamento na conta fiado.
 * Só a ContaFiado cria movimentações (construtor sem "public").
 */
public class MovimentacaoFiado {

    private final LocalDateTime data;
    private final BigDecimal valor;
    private final TipoMovimentacao tipo;
    private final Pedido pedido;

    MovimentacaoFiado(BigDecimal valor, TipoMovimentacao tipo, Pedido pedido) {
        this.data = LocalDateTime.now();
        this.valor = valor;
        this.tipo = tipo;
        this.pedido = pedido;
    }

    public LocalDateTime getData() {
        return data;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    /** Pedido que originou a compra; null quando a movimentação é um pagamento. */
    public Pedido getPedido() {
        return pedido;
    }
}
