package br.com.controleagil.util;

import br.com.controleagil.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static br.com.controleagil.TesteDinheiro.assertValor;
import static br.com.controleagil.TesteDinheiro.reais;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 * Responsabilidade: garantir os formatos brasileiros da tela (R$ 1.234,56, dd/MM/yyyy, HH:mm)
 * e a leitura dos valores digitados.
 */
class FormatadorTest {

    @Test
    @DisplayName("Dinheiro sai no formato R$ 1.234,56")
    void moedaNoPadraoBrasileiro() {
        assertEquals("R$ 1.234,56", Formatador.moeda(reais("1234.56")));
        assertEquals("R$ 5,00", Formatador.moeda(reais("5")));
        assertEquals("R$ 0,00", Formatador.moeda(reais("0")));
    }

    @Test
    @DisplayName("Datas em dd/MM/yyyy, horas em HH:mm e data por extenso em português")
    void datasEHoras() {
        LocalDateTime momento = LocalDateTime.of(2026, 10, 29, 9, 5);
        assertEquals("29/10/2026", Formatador.data(momento.toLocalDate()));
        assertEquals("09:05", Formatador.hora(momento));
        assertEquals("29/10/2026 09:05", Formatador.dataHora(momento));
        assertEquals("quinta-feira, 29 de outubro de 2026", Formatador.dataPorExtenso(LocalDate.of(2026, 10, 29)));
    }

    @Test
    @DisplayName("Lê valores digitados do jeito brasileiro ou com ponto")
    void lerValor() {
        assertValor("12.00", Formatador.lerValor("12"));
        assertValor("12.50", Formatador.lerValor("12,5"));
        assertValor("1234.56", Formatador.lerValor("R$ 1.234,56"));
        assertValor("12.50", Formatador.lerValor("12.50"));
        assertValor("1234.00", Formatador.lerValor("1.234"));
    }

    @Test
    @DisplayName("Valor inválido, negativo ou com mais de 2 casas é recusado com mensagem clara")
    void lerValorInvalido() {
        assertThrows(RegraNegocioException.class, () -> Formatador.lerValor(""));
        assertThrows(RegraNegocioException.class, () -> Formatador.lerValor("abc"));
        assertThrows(RegraNegocioException.class, () -> Formatador.lerValor("-5"));
        assertThrows(RegraNegocioException.class, () -> Formatador.lerValor("12,345"));
    }

    @Test
    @DisplayName("Lê horas no formato HH:mm")
    void lerHora() {
        assertEquals(LocalTime.of(15, 30), Formatador.lerHora("15:30"));
        assertThrows(RegraNegocioException.class, () -> Formatador.lerHora("25:00"));
        assertThrows(RegraNegocioException.class, () -> Formatador.lerHora("15h30"));
    }
}
