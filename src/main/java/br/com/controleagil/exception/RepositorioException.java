package br.com.controleagil.exception;

/*
 * Responsabilidade: representar um erro ao GRAVAR ou LER os dados (ex.: o arquivo do banco está
 * travado por outro programa, o disco encheu). Embrulha a SQLException para quem usa o
 * repositório não precisar tratar SQL.
 *
 * Fica no pacote "exception" (e não no do SQLite) porque a tela precisa reconhecer este erro:
 * quando ele acontece, o sistema avisa que nada foi salvo e recarrega os dados do banco, para
 * a tela nunca mostrar algo que não está gravado.
 *
 * Conceito de POO: HERANÇA (estende RuntimeException) e ENCAPSULAMENTO dos detalhes do banco:
 * a camada de cima recebe uma mensagem em português; a SQLException original vai junto como "causa".
 */
public class RepositorioException extends RuntimeException {

    public RepositorioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
