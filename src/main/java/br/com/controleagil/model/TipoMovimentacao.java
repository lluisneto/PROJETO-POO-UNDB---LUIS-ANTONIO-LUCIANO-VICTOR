package br.com.controleagil.model;

/*
 * Responsabilidade: dizer o que é cada linha do "caderno" do fiado: uma COMPRA (aumenta a dívida),
 * um PAGAMENTO (diminui a dívida) ou um ESTORNO (a compra foi desfeita porque o pedido foi cancelado).
 *
 * Conceito: ENUMERAÇÃO (enum), uma lista fechada de valores.
 */
public enum TipoMovimentacao {

    COMPRA("Compra"),
    PAGAMENTO("Pagamento"),
    /** Desfaz uma compra: o pedido pago no fiado foi cancelado (diminui a dívida, como o pagamento). */
    ESTORNO("Estorno");

    private final String descricao;

    TipoMovimentacao(String descricao) {
        this.descricao = descricao;
    }

    /** Nome para mostrar na tela: "Compra" ou "Pagamento". */
    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
