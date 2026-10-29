package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;

import java.math.BigDecimal;

/*
 * Responsabilidade: uma linha do pedido: qual produto, quantas unidades e por qual preço.
 *
 * Conceito de POO: OBJETO IMUTÁVEL. Todos os atributos são "final": depois de criado, o item
 * não muda. Para "mudar" a quantidade, cria-se um item novo (veja somarQuantidade).
 *
 * Decisão importante: o item guarda o PREÇO DA ÉPOCA (precoUnitario). Se o preço da coxinha
 * subir amanhã, o pedido de hoje continua com o preço de hoje.
 */
public class ItemPedido {

    private final Produto produto;
    private final int quantidade;
    private final BigDecimal precoUnitario;

    /** Cria o item copiando o preço que o produto tem AGORA. */
    public ItemPedido(Produto produto, int quantidade) {
        this(produto, quantidade, produto == null ? null : produto.getPreco());
    }

    /** Construtor privado: só esta classe escolhe um preço diferente do preço atual do produto. */
    private ItemPedido(Produto produto, int quantidade, BigDecimal precoUnitario) {
        if (produto == null) {
            throw new RegraNegocioException("Escolha o produto do item.");
        }
        if (quantidade < 1) {
            throw new RegraNegocioException("A quantidade precisa ser de pelo menos 1 unidade.");
        }
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    /** Subtotal = preço unitário × quantidade (ex.: 3 coxinhas de R$ 5,00 = R$ 15,00). */
    public BigDecimal calcularSubtotal() {
        return Dinheiro.arredondar(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    /**
     * Devolve um item NOVO com mais unidades e o MESMO preço unitário deste.
     * Usado quando o mesmo produto é adicionado de novo ao pedido.
     */
    public ItemPedido somarQuantidade(int quantidadeExtra) {
        if (quantidadeExtra < 1) {
            throw new RegraNegocioException("A quantidade precisa ser de pelo menos 1 unidade.");
        }
        return new ItemPedido(produto, quantidade + quantidadeExtra, precoUnitario);
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
