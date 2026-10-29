package br.com.controleagil.service;

import br.com.controleagil.model.Cliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/*
 * Responsabilidade: uma linha do relatório de devedores: quem deve, quanto e desde quando.
 * "devendoDesde" é a data da compra que tirou o saldo do zero pela última vez.
 *
 * Conceito: RECORD (Java 16+). É uma classe só de dados, imutável: o Java cria sozinho o
 * construtor, os "getters" (cliente(), saldo(), devendoDesde()), equals, hashCode e toString.
 */
public record Devedor(Cliente cliente, BigDecimal saldo, LocalDateTime devendoDesde) {

    /** Há quantos dias o cliente está devendo, contando até "hoje". */
    public long diasDevendo(LocalDate hoje) {
        return ChronoUnit.DAYS.between(devendoDesde.toLocalDate(), hoje);
    }
}
