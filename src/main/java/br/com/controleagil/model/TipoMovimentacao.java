package br.com.controleagil.model;

/*
 * Responsabilidade: dizer o que é cada linha do "caderno" do fiado: uma COMPRA (aumenta a dívida)
 * ou um PAGAMENTO (diminui a dívida).
 *
 * Conceito: ENUMERAÇÃO (enum), uma lista fechada de valores.
 */
public enum TipoMovimentacao {

    COMPRA("Compra"),
    PAGAMENTO("Pagamento");

    private final String descricao;

    TipoMovimentacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
