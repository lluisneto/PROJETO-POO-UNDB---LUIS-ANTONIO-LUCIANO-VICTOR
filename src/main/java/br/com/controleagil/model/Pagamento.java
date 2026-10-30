package br.com.controleagil.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * Responsabilidade: o que TODA forma de pagamento tem em comum: valor e data.
 *
 * Conceitos de POO:
 *  - ABSTRAÇÃO: a classe é "abstract". Não existe "um pagamento qualquer": ele é sempre em
 *    dinheiro, Pix ou fiado. Por isso não dá para fazer "new Pagamento(...)".
 *  - MÉTODO ABSTRATO: processar() não tem corpo aqui. Cada subclasse é OBRIGADA a escrever o seu.
 *  - POLIMORFISMO: o Pedido trabalha com "Pagamento" e chama processar() sem saber o tipo real.
 *    Para criar uma forma nova (ex.: cartão), basta uma subclasse nova; o Pedido não muda.
 */
public abstract class Pagamento {

    private final BigDecimal valor;
    private final LocalDateTime data;

    /** "protected": só as subclasses chamam, com super(valor). */
    protected Pagamento(BigDecimal valor) {
        this.valor = Dinheiro.positivo(valor, "O valor do pagamento precisa ser maior que zero.");
        this.data = LocalDateTime.now();
    }

    /** Valor pago, com 2 casas. */
    public BigDecimal getValor() {
        return valor;
    }

    /** Quando o pagamento foi registrado. */
    public LocalDateTime getData() {
        return data;
    }

    /**
     * Faz o que esta forma de pagamento precisa fazer para ser aceita.
     * Chamado por Pedido.registrarPagamento(). Lança RegraNegocioException se não puder.
     */
    public abstract void processar(Pedido pedido);

    /** Texto para a tela, ex.: "Dinheiro (troco R$ 6,00)". Também é polimórfico: cada forma se descreve. */
    public abstract String getDescricao();
}
