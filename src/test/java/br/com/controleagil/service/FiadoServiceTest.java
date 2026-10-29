package br.com.controleagil.service;

import br.com.controleagil.exception.RegraNegocioException;
import br.com.controleagil.model.Cliente;
import br.com.controleagil.repository.memoria.ClienteRepositoryMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static br.com.controleagil.TesteDinheiro.assertValor;
import static br.com.controleagil.TesteDinheiro.reais;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Responsabilidade: testar o FiadoService: total a receber, devedores ordenados e pagamento.
 */
class FiadoServiceTest {

    private ClienteService clientes;
    private FiadoService fiado;
    private Cliente ana;
    private Cliente bruno;
    private Cliente carla;

    @BeforeEach
    void montarContas() {
        ClienteRepositoryMemoria repositorio = new ClienteRepositoryMemoria();
        clientes = new ClienteService(repositorio);
        fiado = new FiadoService(repositorio);

        ana = clienteComFiado("Ana", "200.00");
        ana.getContaFiado().registrarCompra(reais("120.00"), null);

        bruno = clienteComFiado("Bruno", "100.00");
        bruno.getContaFiado().registrarCompra(reais("50.00"), null);

        carla = clienteComFiado("Carla", "100.00");
        carla.getContaFiado().registrarCompra(reais("30.00"), null);
        carla.getContaFiado().registrarPagamento(reais("30.00"));

        clientes.cadastrar("Davi", "(98) 90000-0004", "Rua D, 4"); // sem conta fiado
    }

    @Test
    @DisplayName("Total a receber soma o saldo devedor de todas as contas")
    void totalAReceber() {
        assertValor("170.00", fiado.totalAReceber());
        assertEquals(2, fiado.contarDevedores());
    }

    @Test
    @DisplayName("A lista de devedores sai ordenada de quem deve mais para quem deve menos")
    void listaDeDevedoresOrdenada() {
        assertEquals(List.of(ana, bruno), fiado.listarContas(true));
        assertEquals(List.of(ana, bruno, carla), fiado.listarContas(false));

        List<Devedor> devedores = fiado.relatorioDevedores();
        assertEquals(List.of(ana, bruno), devedores.stream().map(Devedor::cliente).toList());
        assertNotNull(devedores.get(0).devendoDesde());
        assertEquals(0, devedores.get(0).diasDevendo(LocalDate.now()));
    }

    @Test
    @DisplayName("Pagamento válido reduz o saldo; pagamento maior que o saldo é recusado (RN04)")
    void pagamentoDoFiado() {
        fiado.registrarPagamento(ana, reais("20.00"));
        assertValor("100.00", ana.getContaFiado().getSaldoDevedor());
        assertValor("150.00", fiado.totalAReceber());

        assertThrows(RegraNegocioException.class, () -> fiado.registrarPagamento(ana, reais("100.01")));
        assertValor("100.00", ana.getContaFiado().getSaldoDevedor());
    }

    @Test
    @DisplayName("Uso do limite é a proporção saldo ÷ limite")
    void usoDoLimite() {
        assertValor("0.60", fiado.calcularUsoDoLimite(ana.getContaFiado()));
        assertValor("0", fiado.calcularUsoDoLimite(carla.getContaFiado()));
        assertTrue(fiado.dataUltimaCompra(carla.getContaFiado()).isPresent());
    }

    @Test
    @DisplayName("Quem quitou a dívida não tem data de \"devendo desde\"")
    void quemQuitouNaoDeve() {
        assertNull(FiadoService.devendoDesde(carla.getContaFiado()));
    }

    private Cliente clienteComFiado(String nome, String limite) {
        Cliente cliente = clientes.cadastrar(nome, "(98) 90000-0000", "Rua " + nome);
        clientes.abrirContaFiado(cliente, reais(limite));
        return cliente;
    }
}
