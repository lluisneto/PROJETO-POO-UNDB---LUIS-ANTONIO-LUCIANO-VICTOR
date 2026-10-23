package br.com.controleagil.service;

import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.Pedido;
import br.com.controleagil.model.StatusPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PedidoServiceTest {

    private PedidoService service;
    private Cliente cliente;

    @BeforeEach
    void criarService() {
        service = new PedidoService();
        cliente = new Cliente("Maria", "(98) 90000-0000", "Rua A, 10");
    }

    @Test
    @DisplayName("A agenda mostra só os pedidos do dia, por horário, sem os cancelados")
    void agendaDoDia() {
        LocalDate hoje = LocalDate.now();
        Pedido tarde = service.registrar(new Pedido(cliente, hoje.atTime(15, 0)));
        Pedido manha = service.registrar(new Pedido(cliente, hoje.atTime(9, 0)));
        service.registrar(new Pedido(cliente, hoje.plusDays(1).atTime(10, 0)));
        Pedido cancelado = service.registrar(new Pedido(cliente, hoje.atTime(11, 0)));
        cancelado.alterarStatus(StatusPedido.CANCELADO);

        assertEquals(List.of(manha, tarde), service.agendaDoDia(hoje));
    }

    @Test
    @DisplayName("O alerta pega só as entregas dentro da janela de minutos")
    void entregasProximas() {
        LocalDateTime agora = LocalDateTime.now();
        Pedido daquiAQuinze = service.registrar(new Pedido(cliente, agora.plusMinutes(15)));
        service.registrar(new Pedido(cliente, agora.plusHours(3)));
        Pedido daquiADez = service.registrar(new Pedido(cliente, agora.plusMinutes(10)));

        assertEquals(List.of(daquiADez, daquiAQuinze), service.entregasProximas(30));
    }

    @Test
    @DisplayName("Registrar pedido nulo é recusado")
    void pedidoNuloERecusado() {
        assertThrows(IllegalArgumentException.class, () -> service.registrar(null));
    }
}
