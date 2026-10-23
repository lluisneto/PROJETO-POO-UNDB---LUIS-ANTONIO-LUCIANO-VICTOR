package br.com.controleagil.repository;

/**
 * Erro ao acessar o banco de dados.
 * Embrulha a SQLException para quem usa o repository não precisar tratar SQL.
 */
public class RepositorioException extends RuntimeException {

    public RepositorioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
