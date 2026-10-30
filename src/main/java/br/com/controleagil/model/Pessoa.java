package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;

/*
 * Responsabilidade: guardar os dados comuns a todas as pessoas do sistema: nome e telefone.
 *
 * Conceitos de POO:
 *  - ABSTRAÇÃO: a classe é "abstract", então não dá para fazer "new Pessoa(...)". Ninguém é só
 *    "pessoa": é um Cliente (e, na Etapa 2, um Entregador). Pessoa representa o conceito geral.
 *  - HERANÇA: Cliente "extends Pessoa" e herda nome, telefone e os métodos daqui, sem repetir código.
 *  - ENCAPSULAMENTO: os atributos são "private". De fora, só dá para ler pelos getters e mudar
 *    pelos métodos alterar...(), que validam o valor antes de aceitar.
 */
public abstract class Pessoa {

    private String nome;
    private String telefone;

    /** "protected": só as subclasses (como Cliente) chamam este construtor, com super(...). */
    protected Pessoa(String nome, String telefone) {
        alterarNome(nome);
        alterarTelefone(telefone);
    }

    /** Nome (sempre preenchido). */
    public String getNome() {
        return nome;
    }

    /** Telefone é opcional: pode ser nulo. */
    public String getTelefone() {
        return telefone;
    }

    /**
     * Troca o nome (usado na edição do cadastro). O nome é obrigatório.
     * "final" impede que uma subclasse mude esta regra.
     */
    public final void alterarNome(String novoNome) {
        if (novoNome == null || novoNome.isBlank()) {
            throw new RegraNegocioException("O nome é obrigatório.");
        }
        this.nome = novoNome.strip();
    }

    /** Troca o telefone. Texto vazio vira nulo, para não guardar "  " como telefone. */
    public final void alterarTelefone(String novoTelefone) {
        this.telefone = (novoTelefone == null || novoTelefone.isBlank()) ? null : novoTelefone.strip();
    }
}
