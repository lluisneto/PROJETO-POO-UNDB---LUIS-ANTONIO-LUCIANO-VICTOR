package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;

import java.math.BigDecimal;

/*
 * Responsabilidade: representar um salgado vendido pela distribuidora (nome e preço).
 *
 * Conceito de POO: ENCAPSULAMENTO. Não existe setPreco(): o preço só muda por alterarPreco(),
 * que recusa preço zero ou negativo. Assim nenhum produto fica com preço inválido.
 */
public class Produto {

    private Long id;
    private String nome;
    private BigDecimal preco;

    /** Cria um produto. O nome é obrigatório e o preço precisa ser maior que zero. */
    public Produto(String nome, BigDecimal preco) {
        alterarNome(nome);
        alterarPreco(preco);
    }

    /** Preço ATUAL do produto (os itens de pedidos antigos guardam o preço da época deles). */
    public BigDecimal getPreco() {
        return preco;
    }

    /**
     * Muda o preço do produto daqui para frente.
     * Os pedidos antigos NÃO mudam: cada ItemPedido guardou o preço do dia em que foi vendido.
     *
     * @param novoPreco precisa ser maior que zero
     */
    public final void alterarPreco(BigDecimal novoPreco) {
        this.preco = Dinheiro.positivo(novoPreco, "O preço do produto precisa ser maior que zero.");
    }

    /** Nome do produto (ex.: "Coxinha de frango"). */
    public String getNome() {
        return nome;
    }

    /** Troca o nome do produto. O nome é obrigatório. */
    public final void alterarNome(String novoNome) {
        if (novoNome == null || novoNome.isBlank()) {
            throw new RegraNegocioException("O nome do produto é obrigatório.");
        }
        this.nome = novoNome.strip();
    }

    /** Número do produto, ou null enquanto ele ainda não foi salvo. */
    public Long getId() {
        return id;
    }

    /** Chamado só pelo repositório, quando o produto é salvo pela primeira vez e ganha um número. */
    public void setId(Long id) {
        this.id = id;
    }
}
