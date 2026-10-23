package br.com.controleagil.model;

import java.math.BigDecimal;

/** Salgado vendido pela distribuidora. */
public class Produto {

    private Long id;
    private final String nome;
    private BigDecimal preco;

    public Produto(String nome, BigDecimal preco) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do produto é obrigatório.");
        }
        validarPreco(preco);
        this.nome = nome;
        this.preco = preco;
    }

    public void alterarPreco(BigDecimal novoPreco) {
        validarPreco(novoPreco);
        this.preco = novoPreco;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public String getNome() {
        return nome;
    }

    public Long getId() {
        return id;
    }

    /** Usado pelo repository depois de salvar no banco. */
    public void setId(Long id) {
        this.id = id;
    }

    private void validarPreco(BigDecimal preco) {
        if (preco == null || preco.signum() <= 0) {
            throw new IllegalArgumentException("O preço precisa ser maior que zero.");
        }
    }
}
