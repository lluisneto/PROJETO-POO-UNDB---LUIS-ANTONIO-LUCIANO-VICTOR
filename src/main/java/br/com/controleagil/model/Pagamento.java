package br.com.controleagil.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Base de todas as formas de pagamento (abstração).
 * Cada subclasse implementa processar() do seu jeito (polimorfismo).
 */
public abstract class Pagamento {

    private final BigDecimal valor;
    private final LocalDateTime data;

    protected Pagamento(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor do pagamento precisa ser maior que zero.");
        }
        this.valor = valor;
        this.data = LocalDateTime.now();
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDateTime getData() {
        return data;
    }

    /** Chamado por Pedido.registrarPagamento(). Deve lançar exceção se o pagamento não puder ser feito. */
    public abstract void processar(Pedido pedido);
}
