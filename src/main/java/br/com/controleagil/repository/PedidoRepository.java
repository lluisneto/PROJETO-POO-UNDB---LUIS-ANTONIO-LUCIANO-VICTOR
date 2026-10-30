package br.com.controleagil.repository;

import br.com.controleagil.model.Pedido;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/*
 * Responsabilidade: contrato para guardar e buscar pedidos. O id do pedido é o número do pedido.
 *
 * Conceito de POO: INTERFACE. No sistema: PedidoRepositorySqlite. Nos testes: PedidoRepositoryMemoria.
 */
public interface PedidoRepository {

    /** Pedido sem id é inserido e ganha o próximo número; pedido com id é atualizado. */
    Pedido salvar(Pedido pedido);

    /** Busca pelo número do pedido. Optional vazio quando não existe. */
    Optional<Pedido> buscarPorId(long id);

    /** Todos os pedidos, do horário de entrega mais cedo para o mais tarde. */
    List<Pedido> listarTodos();

    /** Pedidos com entrega no dia informado, por horário de entrega. */
    List<Pedido> listarPorDataEntrega(LocalDate dia);
}
