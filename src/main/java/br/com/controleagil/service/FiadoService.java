package br.com.controleagil.service;

import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.ContaFiado;
import br.com.controleagil.model.MovimentacaoFiado;
import br.com.controleagil.model.TipoMovimentacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Regras do fiado que juntam vários clientes, como o relatório de devedores. */
public class FiadoService {

    /** Clientes com saldo devedor, de quem deve mais para quem deve menos. */
    public List<Devedor> relatorioDevedores(List<Cliente> clientes) {
        return clientes.stream()
                .filter(Cliente::temContaFiado)
                .filter(c -> c.getContaFiado().getSaldoDevedor().signum() > 0)
                .map(c -> new Devedor(c, c.getContaFiado().getSaldoDevedor(), devendoDesde(c.getContaFiado())))
                .sorted(Comparator.comparing(Devedor::saldo).reversed())
                .toList();
    }

    /**
     * Percorre o histórico e guarda a data em que o saldo saiu do zero.
     * Se o cliente quitar tudo, a contagem recomeça na próxima compra.
     * Devolve null quando não há dívida.
     */
    public static LocalDateTime devendoDesde(ContaFiado conta) {
        BigDecimal saldo = BigDecimal.ZERO;
        LocalDateTime desde = null;
        for (MovimentacaoFiado movimentacao : conta.getMovimentacoes()) {
            if (movimentacao.getTipo() == TipoMovimentacao.COMPRA) {
                if (saldo.signum() == 0) {
                    desde = movimentacao.getData();
                }
                saldo = saldo.add(movimentacao.getValor());
            } else {
                saldo = saldo.subtract(movimentacao.getValor());
                if (saldo.signum() == 0) {
                    desde = null;
                }
            }
        }
        return desde;
    }
}
