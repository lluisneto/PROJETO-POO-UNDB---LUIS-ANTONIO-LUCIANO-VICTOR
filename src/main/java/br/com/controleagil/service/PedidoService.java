package br.com.controleagil.service;

import br.com.controleagil.model.Pedido;
import br.com.controleagil.model.StatusPedido;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Regras que olham para vários pedidos ao mesmo tempo: a agenda do dia e os alertas.
 * Enquanto o PedidoRepository não fica pronto, os pedidos ficam guardados em memória.
 */
public class PedidoService {

    private final List<Pedido> pedidos = new ArrayList<>();

    public Pedido registrar(Pedido pedido) {
        if (pedido == null) {
            throw new IllegalArgumentException("Informe o pedido.");
        }
        pedidos.add(pedido);
        return pedido;
    }

    public List<Pedido> listarTodos() {
        return Collections.unmodifiableList(pedidos);
    }

    /** Pedidos com entrega no dia informado, do mais cedo para o mais tarde. Cancelados ficam de fora. */
    public List<Pedido> agendaDoDia(LocalDate dia) {
        return pedidos.stream()
                .filter(p -> p.getStatus() != StatusPedido.CANCELADO)
                .filter(p -> p.getDataHoraEntrega().toLocalDate().equals(dia))
                .sorted(Comparator.comparing(Pedido::getDataHoraEntrega))
                .toList();
    }

    /** Pedidos em aberto que vencem nos próximos minutos (ou que já estão atrasados), por horário. */
    public List<Pedido> entregasProximas(int minutos) {
        return pedidos.stream()
                .filter(p -> p.estaProximoDaEntrega(minutos))
                .sorted(Comparator.comparing(Pedido::getDataHoraEntrega))
                .toList();
    }
}
