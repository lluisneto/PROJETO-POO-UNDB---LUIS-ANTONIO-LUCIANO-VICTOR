package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;

import java.math.BigDecimal;

/*
 * Responsabilidade: representar um cliente da distribuidora, com endereço e,
 * se ele comprar fiado, uma conta fiado.
 *
 * Conceitos de POO:
 *  - HERANÇA: "extends Pessoa". Nome e telefone vêm de Pessoa; aqui só entra o que é de cliente.
 *  - COMPOSIÇÃO: a ContaFiado é criada DENTRO do cliente (abrirContaFiado) e só existe junto dele
 *    (o losango preto do diagrama). Não existe conta fiado "solta", sem dono.
 */
public class Cliente extends Pessoa {

    private Long id;
    private String endereco;
    private ContaFiado contaFiado;

    public Cliente(String nome, String telefone, String endereco) {
        super(nome, telefone); // chama o construtor de Pessoa, que valida o nome
        alterarEndereco(endereco);
    }

    /**
     * Abre a conta fiado do cliente com um limite de crédito.
     * Cada cliente tem no máximo uma conta (multiplicidade 0..1 no diagrama).
     *
     * @param limite quanto o cliente pode ficar devendo (zero ou mais)
     */
    public void abrirContaFiado(BigDecimal limite) {
        if (temContaFiado()) {
            throw new RegraNegocioException("O cliente " + getNome() + " já tem conta fiado.");
        }
        this.contaFiado = new ContaFiado(limite);
    }

    /** Diz se o cliente pode comprar fiado. */
    public boolean temContaFiado() {
        return contaFiado != null;
    }

    /** A conta fiado do cliente, ou null se ele não tiver (confira antes com temContaFiado()). */
    public ContaFiado getContaFiado() {
        return contaFiado;
    }

    public Long getId() {
        return id;
    }

    /** Chamado só pelo repositório, quando o cliente é salvo pela primeira vez e ganha um número. */
    public void setId(Long id) {
        this.id = id;
    }

    /** Endereço é opcional: pode ser nulo. */
    public String getEndereco() {
        return endereco;
    }

    /** Troca o endereço (usado na edição do cadastro). Texto vazio vira nulo. */
    public final void alterarEndereco(String novoEndereco) {
        this.endereco = (novoEndereco == null || novoEndereco.isBlank()) ? null : novoEndereco.strip();
    }
}
