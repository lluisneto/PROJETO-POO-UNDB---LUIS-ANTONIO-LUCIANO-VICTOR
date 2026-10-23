package br.com.controleagil.model;

/**
 * Dados comuns a todas as pessoas do sistema.
 * É abstrata porque ninguém é só "pessoa": é um Cliente (e, na etapa 2, um Entregador).
 */
public abstract class Pessoa {

    private final String nome;
    private final String telefone;

    protected Pessoa(String nome, String telefone) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome é obrigatório.");
        }
        this.nome = nome;
        this.telefone = telefone;
    }

    public String getNome() {
        return nome;
    }

    public String getTelefone() {
        return telefone;
    }
}
