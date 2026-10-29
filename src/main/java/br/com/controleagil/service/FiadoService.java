package br.com.controleagil.service;

import br.com.controleagil.exception.RegraNegocioException;
import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.ContaFiado;
import br.com.controleagil.model.MovimentacaoFiado;
import br.com.controleagil.model.TipoMovimentacao;
import br.com.controleagil.repository.ClienteRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/*
 * Responsabilidade: os casos de uso do fiado que olham para VÁRIAS contas ao mesmo tempo
 * (lista de contas, total a receber, relatório de devedores) e o pagamento da dívida.
 *
 * As regras de UMA conta (limite, saldo, RN03, RN04) ficam na própria ContaFiado.
 * Este service só organiza: busca os clientes, chama a conta e salva.
 */
public class FiadoService {

    /** Quem deve mais aparece primeiro; empate, em ordem alfabética. */
    private static final Comparator<Cliente> POR_SALDO_DECRESCENTE =
            Comparator.comparing((Cliente cliente) -> cliente.getContaFiado().getSaldoDevedor()).reversed()
                    .thenComparing(Cliente::getNome, String.CASE_INSENSITIVE_ORDER);

    private final ClienteRepository clientes;

    public FiadoService(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    /**
     * Clientes que têm conta fiado, de quem deve mais para quem deve menos.
     *
     * @param somenteQuemDeve true para esconder quem está com saldo zero
     */
    public List<Cliente> listarContas(boolean somenteQuemDeve) {
        return clientes.listarTodos().stream()
                .filter(Cliente::temContaFiado)
                .filter(cliente -> !somenteQuemDeve || cliente.getContaFiado().getSaldoDevedor().signum() > 0)
                .sorted(POR_SALDO_DECRESCENTE)
                .toList();
    }

    /** RF12: relatório de devedores (quem deve, quanto e desde quando), de todos os clientes. */
    public List<Devedor> relatorioDevedores() {
        return relatorioDevedores(clientes.listarTodos());
    }

    /** Relatório de devedores a partir de uma lista de clientes (quem deve mais vem primeiro). */
    public List<Devedor> relatorioDevedores(List<Cliente> lista) {
        return lista.stream()
                .filter(Cliente::temContaFiado)
                .filter(cliente -> cliente.getContaFiado().getSaldoDevedor().signum() > 0)
                .sorted(POR_SALDO_DECRESCENTE)
                .map(cliente -> new Devedor(cliente, cliente.getContaFiado().getSaldoDevedor(),
                        devendoDesde(cliente.getContaFiado())))
                .toList();
    }

    /** Soma do saldo devedor de todas as contas: quanto a distribuidora tem para receber. */
    public BigDecimal totalAReceber() {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Cliente cliente : listarContas(true)) {
            total = total.add(cliente.getContaFiado().getSaldoDevedor());
        }
        return total;
    }

    /** Quantos clientes estão devendo alguma coisa. */
    public long contarDevedores() {
        return listarContas(true).size();
    }

    /**
     * RF09: o cliente paga (total ou parte) da dívida do fiado.
     * A RN04 (valor maior que zero e no máximo o saldo) é conferida pela ContaFiado.
     */
    public void registrarPagamento(Cliente cliente, BigDecimal valor) {
        if (!cliente.temContaFiado()) {
            throw new RegraNegocioException("O cliente " + cliente.getNome() + " não tem conta fiado.");
        }
        cliente.getContaFiado().registrarPagamento(valor);
        clientes.salvar(cliente);
    }

    /** Data da compra fiado mais recente, se houver. */
    public Optional<LocalDateTime> dataUltimaCompra(ContaFiado conta) {
        return conta.getMovimentacoes().stream()
                .filter(movimentacao -> movimentacao.getTipo() == TipoMovimentacao.COMPRA)
                .map(MovimentacaoFiado::getData)
                .max(Comparator.naturalOrder());
    }

    /**
     * Quanto do limite já foi usado, de 0 (nada) a 1 (tudo). Ex.: deve R$ 120 de R$ 200 = 0,60.
     * Não é dinheiro, é uma proporção (serve para a barra de uso do limite).
     */
    public BigDecimal calcularUsoDoLimite(ContaFiado conta) {
        BigDecimal saldo = conta.getSaldoDevedor();
        if (conta.getLimiteCredito().signum() == 0) {
            return saldo.signum() > 0 ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        BigDecimal uso = saldo.divide(conta.getLimiteCredito(), 4, RoundingMode.HALF_EVEN);
        return uso.min(BigDecimal.ONE);
    }

    /**
     * Percorre o histórico e guarda a data em que o saldo saiu do zero pela última vez.
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
