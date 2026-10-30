package br.com.controleagil;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * Responsabilidade: atalhos para os testes que mexem com dinheiro.
 *
 * Por que assertValor e não assertEquals direto? Para o BigDecimal, 5.0 e 5.00 são DIFERENTES
 * no equals (a escala é outra), mas iguais no compareTo. Em dinheiro, o que importa é o valor.
 */
public final class TesteDinheiro {

    private TesteDinheiro() {
    }

    /** reais("5.50") é mais curto de ler que new BigDecimal("5.50"). */
    public static BigDecimal reais(String valor) {
        return new BigDecimal(valor);
    }

    /** Confere se dois valores em dinheiro são iguais, usando compareTo. */
    public static void assertValor(String esperado, BigDecimal atual) {
        assertValor(esperado, atual, "");
    }

    /** Igual ao de cima, com uma explicação extra se falhar. */
    public static void assertValor(String esperado, BigDecimal atual, String explicacao) {
        assertEquals(0, new BigDecimal(esperado).compareTo(atual),
                "esperado R$ " + esperado + ", veio R$ " + atual + (explicacao.isEmpty() ? "" : " (" + explicacao + ")"));
    }
}
