package br.com.controleagil.service;

import br.com.controleagil.model.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FiadoServiceTest {

    private final FiadoService service = new FiadoService();

    @Test
    @DisplayName("O relatório lista só quem deve, de quem deve mais para quem deve menos")
    void relatorioDeDevedores() {
        Cliente ana = clienteComFiado("Ana", "200.00");
        ana.getContaFiado().registrarCompra(new BigDecimal("120.00"), null);

        Cliente bruno = clienteComFiado("Bruno", "100.00");
        bruno.getContaFiado().registrarCompra(new BigDecimal("50.00"), null);

        Cliente carla = clienteComFiado("Carla", "100.00");
        carla.getContaFiado().registrarCompra(new BigDecimal("30.00"), null);
        carla.getContaFiado().registrarPagamento(new BigDecimal("30.00"));

        Cliente davi = new Cliente("Davi", "(98) 90000-0004", "Rua D, 4");

        List<Devedor> devedores = service.relatorioDevedores(List.of(bruno, carla, davi, ana));

        assertEquals(List.of(ana, bruno), devedores.stream().map(Devedor::cliente).toList());
        assertEquals(0, new BigDecimal("120.00").compareTo(devedores.get(0).saldo()));
        assertNotNull(devedores.get(0).devendoDesde());
        assertEquals(0, devedores.get(0).diasDevendo(LocalDate.now()));
    }

    @Test
    @DisplayName("Quem quitou a dívida não tem data de \"devendo desde\"")
    void quemQuitouNaoDeve() {
        Cliente carla = clienteComFiado("Carla", "100.00");
        carla.getContaFiado().registrarCompra(new BigDecimal("30.00"), null);
        carla.getContaFiado().registrarPagamento(new BigDecimal("30.00"));

        assertNull(FiadoService.devendoDesde(carla.getContaFiado()));
    }

    private Cliente clienteComFiado(String nome, String limite) {
        Cliente cliente = new Cliente(nome, "(98) 90000-0000", "Rua " + nome);
        cliente.abrirContaFiado(new BigDecimal(limite));
        return cliente;
    }
}
