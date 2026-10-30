package br.com.controleagil.service;

import br.com.controleagil.exception.RegraNegocioException;
import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.ItemPedido;
import br.com.controleagil.model.Pagamento;
import br.com.controleagil.model.PagamentoCartao;
import br.com.controleagil.model.Pedido;
import br.com.controleagil.model.Produto;
import br.com.controleagil.model.StatusPedido;
import br.com.controleagil.model.TipoCartao;
import br.com.controleagil.repository.memoria.PedidoRepositoryMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static br.com.controleagil.TesteDinheiro.assertValor;
import static br.com.controleagil.TesteDinheiro.reais;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Responsabilidade: testar o PedidoService.
 * O relógio é FIXO (quinta, 29/10/2026, 10:00), então "agora" é sempre o mesmo e os testes
 * de atraso e de data no passado não dependem da hora em que são rodados.
 */
class PedidoServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 29, 10, 0);
    private static final LocalDate HOJE = AGORA.toLocalDate();

    private PedidoService service;
    private Cliente cliente;
    private Produto coxinha;

    @BeforeEach
    void criarService() {
        Clock relogio = Clock.fixed(AGORA.atZone(FUSO).toInstant(), FUSO);
        service = new PedidoService(new PedidoRepositoryMemoria(), relogio);
        cliente = new Cliente("Maria", "(98) 90000-0000", "Rua A, 10");
        coxinha = new Produto("Coxinha", reais("5.00"));
    }

    @Test
    @DisplayName("Pedido válido é salvo e ganha número")
    void pedidoValidoEhSalvo() {
        Pedido pedido = service.registrarPedido(cliente, AGORA.plusHours(1), List.of(new ItemPedido(coxinha, 3)), null);

        assertNotNull(pedido.getId());
        assertValor("15.00", pedido.calcularTotal());
        assertEquals(1, service.listarTodos().size());
    }

    @Test
    @DisplayName("RN02: pedido sem itens não é salvo")
    void pedidoSemItensNaoEhSalvo() {
        assertThrows(RegraNegocioException.class,
                () -> service.registrarPedido(cliente, AGORA.plusHours(1), List.of(), null));
        assertTrue(service.listarTodos().isEmpty());
    }

    @Test
    @DisplayName("Data de entrega no passado é recusada")
    void dataNoPassadoEhRecusada() {
        assertThrows(RegraNegocioException.class,
                () -> service.registrarPedido(cliente, AGORA.minusMinutes(1), List.of(new ItemPedido(coxinha, 1)), null));
        assertTrue(service.listarTodos().isEmpty());
    }

    @Test
    @DisplayName("RN01: pedido sem data e hora de entrega é recusado")
    void pedidoSemDataEhRecusado() {
        assertThrows(RegraNegocioException.class,
                () -> service.registrarPedido(cliente, null, List.of(new ItemPedido(coxinha, 1)), null));
    }

    @Test
    @DisplayName("Fiado acima do limite é salvo (RN03 só avisa, na tela) e a dívida passa do limite")
    void fiadoAcimaDoLimiteEhSalvo() {
        cliente.abrirContaFiado(reais("10.00"));
        List<ItemPedido> itens = List.of(new ItemPedido(coxinha, 3)); // R$ 15,00

        Pedido pedido = service.registrarPedido(cliente, AGORA.plusHours(1), itens,
                service.criarPagamento(FormaPagamento.FIADO, service.calcularTotal(itens), null, null));
        assertNotNull(pedido.getId());
        assertValor("15.00", cliente.getContaFiado().getSaldoDevedor());
        assertValor("5.00", cliente.getContaFiado().getValorAcimaDoLimite());
    }

    @Test
    @DisplayName("Fiado sem conta continua recusado: nada é salvo")
    void fiadoSemContaNaoSalvaOPedido() {
        List<ItemPedido> itens = List.of(new ItemPedido(coxinha, 3));
        assertThrows(RegraNegocioException.class, () -> service.registrarPedido(cliente, AGORA.plusHours(1), itens,
                service.criarPagamento(FormaPagamento.FIADO, service.calcularTotal(itens), null, null)));
        assertTrue(service.listarTodos().isEmpty());
    }

    @Test
    @DisplayName("A forma escolhida na tela vira a subclasse certa de Pagamento (cartão de débito e de crédito)")
    void criarPagamentoNoCartao() {
        Pagamento debito = service.criarPagamento(FormaPagamento.CARTAO_DEBITO, reais("10.00"), null, null);
        Pagamento credito = service.criarPagamento(FormaPagamento.CARTAO_CREDITO, reais("10.00"), null, null);

        assertEquals(TipoCartao.DEBITO, assertInstanceOf(PagamentoCartao.class, debito).getTipo());
        assertEquals(TipoCartao.CREDITO, assertInstanceOf(PagamentoCartao.class, credito).getTipo());
    }

    @Test
    @DisplayName("No rascunho, adicionar o mesmo produto soma a quantidade")
    void rascunhoSomaQuantidade() {
        List<ItemPedido> itens = service.adicionarAoRascunho(List.of(), coxinha, 2);
        itens = service.adicionarAoRascunho(itens, coxinha, 3);

        assertEquals(1, itens.size());
        assertEquals(5, itens.get(0).getQuantidade());
        assertValor("25.00", service.calcularTotal(itens));
    }

    @Test
    @DisplayName("Avançar status segue a ordem e para no Entregue (RN05)")
    void avancarStatus() {
        Pedido pedido = service.registrarPedido(cliente, AGORA.plusHours(1), List.of(new ItemPedido(coxinha, 1)), null);

        service.avancarStatus(pedido);
        assertEquals(StatusPedido.EM_PRODUCAO, pedido.getStatus());
        service.avancarStatus(pedido);
        service.avancarStatus(pedido);
        assertEquals(StatusPedido.ENTREGUE, pedido.getStatus());
        assertThrows(RegraNegocioException.class, () -> service.avancarStatus(pedido));
        assertThrows(RegraNegocioException.class, () -> service.cancelar(pedido));
    }

    @Test
    @DisplayName("Pedidos do dia saem por horário e o filtro de status funciona")
    void pedidosDoDia() {
        Pedido tarde = service.registrarPedido(cliente, HOJE.atTime(15, 0), List.of(new ItemPedido(coxinha, 1)), null);
        Pedido manha = service.registrarPedido(cliente, HOJE.atTime(11, 0), List.of(new ItemPedido(coxinha, 1)), null);
        service.registrarPedido(cliente, HOJE.plusDays(1).atTime(9, 0), List.of(new ItemPedido(coxinha, 1)), null);
        Pedido cancelado = service.registrarPedido(cliente, HOJE.atTime(12, 0), List.of(new ItemPedido(coxinha, 1)), null);
        service.cancelar(cancelado);

        assertEquals(List.of(manha, cancelado, tarde), service.pedidosDoDia(HOJE, null));
        assertEquals(List.of(cancelado), service.pedidosDoDia(HOJE, StatusPedido.CANCELADO));
        assertEquals(List.of(manha, tarde), service.agendaDoDia(HOJE));
    }

    @Test
    @DisplayName("Em breve: entrega nos próximos 60 minutos; atrasado: horário já passou")
    void emBreveEAtrasado() {
        Pedido daquiA30 = service.registrarPedido(cliente, AGORA.plusMinutes(30), List.of(new ItemPedido(coxinha, 1)), null);
        Pedido daquiA3h = service.registrarPedido(cliente, AGORA.plusHours(3), List.of(new ItemPedido(coxinha, 1)), null);
        Pedido jaPassou = service.comRelogio(Clock.fixed(AGORA.minusHours(2).atZone(FUSO).toInstant(), FUSO))
                .registrarPedido(cliente, AGORA.minusMinutes(20), List.of(new ItemPedido(coxinha, 1)), null);

        assertTrue(service.estaEmBreve(daquiA30));
        assertFalse(service.estaEmBreve(daquiA3h));
        assertTrue(service.estaAtrasado(jaPassou));
        assertFalse(service.estaEmBreve(jaPassou), "atrasado não é \"em breve\"");
        assertEquals(1, service.contarAtrasados());
        assertEquals(2, service.contarEntregasNasProximasHoras(3));
    }

    @Test
    @DisplayName("Atrasados de dias anteriores aparecem primeiro nas próximas entregas")
    void atrasadosDeOutrosDias() {
        PedidoService ontem = service.comRelogio(Clock.fixed(AGORA.minusDays(1).minusHours(2).atZone(FUSO).toInstant(), FUSO));
        Pedido esquecido = ontem.registrarPedido(cliente, AGORA.minusDays(1), List.of(new ItemPedido(coxinha, 1)), null);
        Pedido deHoje = service.registrarPedido(cliente, AGORA.plusHours(1), List.of(new ItemPedido(coxinha, 1)), null);

        assertEquals(List.of(esquecido), service.atrasadosDeOutrosDias());
        assertEquals(List.of(esquecido, deHoje), service.proximasEntregas(5));
        assertEquals(List.of(esquecido), service.atrasados());

        service.avancarStatus(esquecido);
        assertEquals(List.of(esquecido), service.atrasados(), "em produção continua atrasado");
        service.cancelar(esquecido);
        assertTrue(service.atrasados().isEmpty(), "cancelado não está mais atrasado");
    }

    @Test
    @DisplayName("Pedidos por dia (gráfico do Início) e entregues de hoje não contam os cancelados")
    void pedidosPorDiaEEntreguesDeHoje() {
        Pedido entregue = service.registrarPedido(cliente, AGORA.plusHours(1), List.of(new ItemPedido(coxinha, 1)), null);
        service.registrarPedido(cliente, AGORA.plusHours(2), List.of(new ItemPedido(coxinha, 1)), null);
        service.cancelar(service.registrarPedido(cliente, AGORA.plusHours(3), List.of(new ItemPedido(coxinha, 1)), null));
        service.registrarPedido(cliente, HOJE.plusDays(2).atTime(9, 0), List.of(new ItemPedido(coxinha, 1)), null);
        while (entregue.getStatus() != StatusPedido.ENTREGUE) {
            service.avancarStatus(entregue);
        }

        assertEquals(List.of(2L, 0L, 1L), List.copyOf(service.pedidosPorDia(HOJE, 3).values()));
        assertEquals(List.of(HOJE, HOJE.plusDays(1), HOJE.plusDays(2)), List.copyOf(service.pedidosPorDia(HOJE, 3).keySet()));
        assertEquals(1, service.contarEntreguesDeHoje());
    }

    @Test
    @DisplayName("Pagamento registrado depois diminui o valor restante")
    void pagamentoDepois() {
        Pedido pedido = service.registrarPedido(cliente, AGORA.plusHours(1), List.of(new ItemPedido(coxinha, 4)), null);

        service.registrarPagamento(pedido, service.criarPagamento(FormaPagamento.PIX, reais("8.00"), null, null));

        assertValor("12.00", service.calcularValorRestante(pedido));
        assertValor("2.00", service.calcularTroco(reais("8.00"), reais("10.00")));
    }
}
