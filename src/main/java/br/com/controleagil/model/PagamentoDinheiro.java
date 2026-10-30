package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * Responsabilidade: pagamento em dinheiro vivo: guarda quanto o cliente entregou e calcula o troco.
 *
 * Conceitos de POO: HERANÇA ("extends Pagamento") e POLIMORFISMO (processar() do seu jeito:
 * confere se o dinheiro entregue cobre o valor).
 */
public class PagamentoDinheiro extends Pagamento {

    private final BigDecimal valorRecebido;

    /**
     * @param valor         quanto deste pedido está sendo pago
     * @param valorRecebido quanto o cliente entregou em dinheiro (pode ser mais, e aí tem troco)
     */
    public PagamentoDinheiro(BigDecimal valor, BigDecimal valorRecebido) {
        this(valor, valorRecebido, LocalDateTime.now());
    }

    /** Usado ao remontar um pagamento salvo no banco, com a data em que ele foi feito. */
    public PagamentoDinheiro(BigDecimal valor, BigDecimal valorRecebido, LocalDateTime data) {
        super(valor, data);
        if (valorRecebido == null) {
            throw new RegraNegocioException("Informe o valor recebido em dinheiro.");
        }
        this.valorRecebido = Dinheiro.arredondar(valorRecebido);
    }

    /** Troco = valor recebido − valor do pagamento (ex.: pagou R$ 64,00 com R$ 70,00: troco de R$ 6,00). */
    public BigDecimal calcularTroco() {
        return valorRecebido.subtract(getValor());
    }

    /** Dinheiro: o valor recebido precisa ser pelo menos o valor do pagamento. */
    @Override
    public void processar(Pedido pedido) {
        if (valorRecebido.compareTo(getValor()) < 0) {
            throw new RegraNegocioException("O valor recebido (" + Dinheiro.formatar(valorRecebido)
                    + ") é menor que o valor a pagar (" + Dinheiro.formatar(getValor()) + ").");
        }
    }

    /** "Dinheiro" ou "Dinheiro (troco R$ 6,00)". */
    @Override
    public String getDescricao() {
        BigDecimal troco = calcularTroco();
        return troco.signum() > 0 ? "Dinheiro (troco " + Dinheiro.formatar(troco) + ")" : "Dinheiro";
    }

    /** Quanto o cliente entregou em dinheiro. */
    public BigDecimal getValorRecebido() {
        return valorRecebido;
    }
}
