package br.com.controleagil.model;

import java.math.BigDecimal;

/** Pagamento em dinheiro: confere o valor recebido e calcula o troco. */
public class PagamentoDinheiro extends Pagamento {

    private final BigDecimal valorRecebido;

    public PagamentoDinheiro(BigDecimal valor, BigDecimal valorRecebido) {
        super(valor);
        if (valorRecebido == null) {
            throw new IllegalArgumentException("Informe o valor recebido.");
        }
        this.valorRecebido = valorRecebido;
    }

    public BigDecimal calcularTroco() {
        return valorRecebido.subtract(getValor());
    }

    @Override
    public void processar(Pedido pedido) {
        if (valorRecebido.compareTo(getValor()) < 0) {
            throw new IllegalStateException("Valor recebido (R$ " + valorRecebido
                    + ") é menor que o valor do pagamento (R$ " + getValor() + ").");
        }
    }

    public BigDecimal getValorRecebido() {
        return valorRecebido;
    }
}
