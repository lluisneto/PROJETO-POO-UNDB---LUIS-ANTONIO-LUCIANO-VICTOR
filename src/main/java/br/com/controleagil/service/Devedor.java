package br.com.controleagil.service;

import br.com.controleagil.model.Cliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Uma linha do relatório de devedores: quem deve, quanto e desde quando.
 * "devendoDesde" é a data da compra que tirou o saldo do zero pela última vez.
 */
public record Devedor(Cliente cliente, BigDecimal saldo, LocalDateTime devendoDesde) {

    public long diasDevendo(LocalDate hoje) {
        return ChronoUnit.DAYS.between(devendoDesde.toLocalDate(), hoje);
    }
}
