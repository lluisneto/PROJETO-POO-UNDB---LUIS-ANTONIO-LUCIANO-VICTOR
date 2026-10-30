package br.com.controleagil.model;

/*
 * Responsabilidade: dizer se o pagamento no cartão foi no DÉBITO ou no CRÉDITO.
 *
 * Conceito: ENUMERAÇÃO (enum), uma lista fechada de valores.
 */
public enum TipoCartao {

    DEBITO("Cartão de débito"),
    CREDITO("Cartão de crédito");

    private final String descricao;

    TipoCartao(String descricao) {
        this.descricao = descricao;
    }

    /** Nome para mostrar na tela, ex.: "Cartão de débito". */
    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
