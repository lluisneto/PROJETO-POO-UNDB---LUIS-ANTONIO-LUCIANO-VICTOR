package br.com.controleagil.repository.sqlite;

/*
 * Responsabilidade: representar um erro ao acessar o banco de dados.
 * Embrulha a SQLException para quem usa o repositório não precisar tratar SQL.
 *
 * Conceito de POO: HERANÇA (estende RuntimeException) e ENCAPSULAMENTO dos detalhes do banco:
 * a camada de cima recebe uma mensagem em português; a SQLException original vai junto como "causa".
 */
public class RepositorioException extends RuntimeException {

    public RepositorioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
