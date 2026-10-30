package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * Responsabilidade: pagamento na maquininha, no cartão de DÉBITO ou de CRÉDITO.
 *
 * Conceitos de POO: HERANÇA ("extends Pagamento") e POLIMORFISMO (processar() do seu jeito:
 * só confere se o tipo do cartão foi informado; a aprovação em si acontece na maquininha).
 * Repare que o Pedido não precisou mudar nada para aceitar esta forma nova de pagamento:
 * ele só conhece "Pagamento". Essa é a vantagem do polimorfismo.
 */
public class PagamentoCartao extends Pagamento {

    private final TipoCartao tipo;

    /** @param tipo DEBITO ou CREDITO */
    public PagamentoCartao(BigDecimal valor, TipoCartao tipo) {
        this(valor, tipo, LocalDateTime.now());
    }

    /** Usado ao remontar um pagamento salvo no banco, com a data em que ele foi feito. */
    public PagamentoCartao(BigDecimal valor, TipoCartao tipo, LocalDateTime data) {
        super(valor, data);
        this.tipo = tipo;
    }

    /** Cartão: precisa dizer se foi débito ou crédito. */
    @Override
    public void processar(Pedido pedido) {
        if (tipo == null) {
            throw new RegraNegocioException("Informe se o cartão é de débito ou de crédito.");
        }
    }

    /** "Cartão de débito" ou "Cartão de crédito". */
    @Override
    public String getDescricao() {
        return tipo == null ? "Cartão" : tipo.getDescricao();
    }

    /** DEBITO ou CREDITO. */
    public TipoCartao getTipo() {
        return tipo;
    }
}
