package br.com.controleagil.model;

import java.math.BigDecimal;

/**
 * Cliente da distribuidora. Herda nome e telefone de Pessoa.
 * Pode ter (ou não) uma conta fiado.
 */
public class Cliente extends Pessoa {

    private Long id;
    private final String endereco;
    private ContaFiado contaFiado;

    public Cliente(String nome, String telefone, String endereco) {
        super(nome, telefone);
        this.endereco = endereco;
    }

    public void abrirContaFiado(BigDecimal limite) {
        if (temContaFiado()) {
            throw new IllegalStateException("O cliente " + getNome() + " já possui conta fiado.");
        }
        this.contaFiado = new ContaFiado(limite);
    }

    public boolean temContaFiado() {
        return contaFiado != null;
    }

    public ContaFiado getContaFiado() {
        return contaFiado;
    }

    public Long getId() {
        return id;
    }

    /** Usado pelo repository depois de salvar no banco. */
    public void setId(Long id) {
        this.id = id;
    }

    public String getEndereco() {
        return endereco;
    }
}
