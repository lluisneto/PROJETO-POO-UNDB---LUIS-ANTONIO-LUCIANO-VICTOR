package br.com.controleagil.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * Responsabilidade: uma linha do "caderno" do fiado: quando, quanto, se foi compra ou pagamento
 * e, nas compras, de qual pedido veio.
 *
 * Conceito de POO: ENCAPSULAMENTO. O construtor não tem "public": só classes do pacote model
 * (na prática, a ContaFiado) criam movimentações. Assim ninguém consegue inventar uma linha no
 * caderno sem passar pelas regras da conta (limite, saldo...).
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

    /** Pedido que originou a compra; null quando a movimentação é um pagamento da dívida. */
    public Pedido getPedido() {
        return pedido;
    }
}
