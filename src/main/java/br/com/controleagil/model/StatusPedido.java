package br.com.controleagil.model;

/**
 * Etapas de um pedido, na ordem em que acontecem:
 * PENDENTE → EM_PRODUCAO → SAIU_PARA_ENTREGA → ENTREGUE (ou CANCELADO).
 */
public enum StatusPedido {
    PENDENTE,
    EM_PRODUCAO,
    SAIU_PARA_ENTREGA,
    ENTREGUE,
    CANCELADO;

    /** Um pedido entregue ou cancelado não muda mais de status. */
    public boolean isFinal() {
        return this == ENTREGUE || this == CANCELADO;
    }
}
