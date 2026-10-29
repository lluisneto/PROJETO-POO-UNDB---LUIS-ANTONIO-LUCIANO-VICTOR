package br.com.controleagil.repository.memoria;

import br.com.controleagil.model.Pedido;
import br.com.controleagil.repository.PedidoRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/*
 * Responsabilidade: guardar os pedidos na memória (some ao fechar o sistema).
 * O id gerado aqui (1, 2, 3...) é o número do pedido que aparece na tela.
 *
 * Conceito de POO: IMPLEMENTAÇÃO DE INTERFACE ("implements PedidoRepository").
 */
public class PedidoRepositoryMemoria implements PedidoRepository {

    private static final Comparator<Pedido> POR_HORARIO_DE_ENTREGA =
            Comparator.comparing(Pedido::getDataHoraEntrega).thenComparing(Pedido::getId);

    private final Map<Long, Pedido> pedidos = new LinkedHashMap<>();
    private final AtomicLong proximoId = new AtomicLong(1);

    @Override
    public Pedido salvar(Pedido pedido) {
        if (pedido.getId() == null) {
            pedido.setId(proximoId.getAndIncrement());
        }
        pedidos.put(pedido.getId(), pedido);
        return pedido;
    }

    @Override
    public Optional<Pedido> buscarPorId(long id) {
        return Optional.ofNullable(pedidos.get(id));
    }

    @Override
    public List<Pedido> listarTodos() {
        return pedidos.values().stream()
                .sorted(POR_HORARIO_DE_ENTREGA)
                .toList(); // toList() já devolve uma lista somente leitura
    }

    @Override
    public List<Pedido> listarPorDataEntrega(LocalDate dia) {
        return pedidos.values().stream()
                .filter(pedido -> pedido.getDataHoraEntrega().toLocalDate().equals(dia))
                .sorted(POR_HORARIO_DE_ENTREGA)
                .toList();
    }
}
