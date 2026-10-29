package br.com.controleagil.exception;

/*
 * Responsabilidade: representar a quebra de uma REGRA DE NEGÓCIO
 * (ex.: compra fiado acima do limite, pedido sem itens).
 *
 * Conceito de POO: HERANÇA. Ela herda de RuntimeException, então ganha de graça tudo o que
 * uma exceção tem (mensagem, pilha de chamadas...). Ser "Runtime" significa que o Java não
 * obriga a declarar "throws" em cada método: quem quiser trata, quem não quiser deixa subir.
 *
 * Por que uma exceção só nossa? Para a tela saber a diferença entre:
 *  - RegraNegocioException: o usuário fez algo que o negócio não permite. A mensagem já está
 *    em português claro e vira um aviso amigável na tela;
 *  - qualquer outra exceção: erro inesperado (bug). Mostra um aviso genérico e o detalhe
 *    técnico vai para o console.
 */
public class RegraNegocioException extends RuntimeException {

    /** @param mensagem texto em português dizendo o que está errado e, se der, como corrigir */
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
