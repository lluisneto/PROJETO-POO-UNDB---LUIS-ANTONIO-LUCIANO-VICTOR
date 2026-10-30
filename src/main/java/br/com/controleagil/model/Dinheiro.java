package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/*
 * Responsabilidade: guardar num lugar só as regras de dinheiro do model:
 * sempre 2 casas decimais e arredondamento HALF_EVEN.
 *
 * Classe de apoio (não aparece no diagrama): não tem "public" na frente, então só as classes
 * do próprio pacote model enxergam. Isso também é encapsulamento, só que no nível do pacote.
 *
 * Por que BigDecimal e não double? O double guarda números em binário e erra contas simples:
 * em Java, 0.1 + 0.2 dá 0.30000000000000004. Em dinheiro, isso vira centavo sumindo.
 *
 * Por que HALF_EVEN? É o "arredondamento do banqueiro": quando o número está exatamente no meio
 * (ex.: 2,345), arredonda para o vizinho PAR (2,34). Em muitas contas, os arredondamentos para
 * cima e para baixo se compensam, e a soma final não fica "puxada" para cima.
 */
final class Dinheiro {

    /** Zero já com 2 casas: 0.00 */
    static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

    private static final Locale BRASIL = Locale.of("pt", "BR");

    /**
     * O Java separa "R$" do número com um espaço especial, que não quebra linha (código 160, U+00A0).
     * Ele é invisível no editor, por isso fica numa constante com nome.
     */
    private static final char ESPACO_QUE_NAO_QUEBRA = (char) 160;

    private Dinheiro() {
        // Só tem métodos estáticos: ninguém precisa criar um objeto Dinheiro.
    }

    /** Deixa o valor com 2 casas (ex.: 5 vira 5.00; 2.345 vira 2.34). Nulo continua nulo. */
    static BigDecimal arredondar(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Arredonda e SÓ DEPOIS confere se é maior que zero. A ordem importa: 0,004 é maior que zero,
     * mas com 2 casas vira 0,00, e um preço ou pagamento de R$ 0,00 não pode passar.
     *
     * @param mensagem texto do erro se o valor for nulo, zero ou negativo
     */
    static BigDecimal positivo(BigDecimal valor, String mensagem) {
        BigDecimal arredondado = arredondar(valor);
        if (arredondado == null || arredondado.signum() <= 0) {
            throw new RegraNegocioException(mensagem);
        }
        return arredondado;
    }

    /** Texto para as mensagens de erro, no padrão brasileiro: R$ 1.234,56 */
    static String formatar(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(BRASIL).format(valor).replace(ESPACO_QUE_NAO_QUEBRA, ' ');
    }
}
