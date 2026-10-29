package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;

/*
 * Responsabilidade: listar as etapas de um pedido, na ordem em que acontecem:
 * PENDENTE → EM_PRODUCAO → SAIU_PARA_ENTREGA → ENTREGUE (ou CANCELADO, a qualquer momento antes do fim).
 *
 * Conceito: ENUMERAÇÃO (enum). É uma lista fechada de valores. O Java não deixa criar um
 * status que não esteja aqui, e cada valor pode ter atributos e métodos, como qualquer objeto.
 */
public enum StatusPedido {

    PENDENTE("Pendente"),
    EM_PRODUCAO("Em produção"),
    SAIU_PARA_ENTREGA("Saiu para entrega"),
    ENTREGUE("Entregue"),
    CANCELADO("Cancelado");

    /** Nome bonito para mostrar na tela ("Em produção" em vez de "EM_PRODUCAO"). */
    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    /** RN05: um pedido ENTREGUE ou CANCELADO não muda mais de status. */
    public boolean isFinal() {
        return this == ENTREGUE || this == CANCELADO;
    }

    /**
     * Para qual status o pedido avança a partir deste (CANCELADO não entra: é uma saída à parte).
     * Só faz sentido para status que não são finais.
     */
    public StatusPedido proximo() {
        return switch (this) {
            case PENDENTE -> EM_PRODUCAO;
            case EM_PRODUCAO -> SAIU_PARA_ENTREGA;
            case SAIU_PARA_ENTREGA -> ENTREGUE;
            case ENTREGUE, CANCELADO -> throw new RegraNegocioException(
                    "Um pedido " + descricao.toLowerCase() + " não avança mais de status.");
        };
    }

    public String getDescricao() {
        return descricao;
    }

    /** O texto do enum na tela e nas mensagens é a descrição. O name() continua "EM_PRODUCAO". */
    @Override
    public String toString() {
        return descricao;
    }
}
