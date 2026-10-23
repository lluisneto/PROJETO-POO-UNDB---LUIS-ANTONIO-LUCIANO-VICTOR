package br.com.controleagil.model;

import java.math.BigDecimal;

/**
 * Uma linha do pedido: produto + quantidade.
 * Guarda o preço do momento da venda, para o pedido não mudar se o preço do produto mudar depois.
 */
public class ItemPedido {

    private final Produto produto;
    private final int quantidade;
    private final BigDecimal precoUnitario;

    public ItemPedido(Produto produto, int quantidade) {
        if (produto == null) {
            throw new IllegalArgumentException("O item precisa de um produto.");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade precisa ser maior que zero.");
        }
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = produto.getPreco();
    }

    public BigDecimal calcularSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public Produto getProduto() {
        return produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }
}
