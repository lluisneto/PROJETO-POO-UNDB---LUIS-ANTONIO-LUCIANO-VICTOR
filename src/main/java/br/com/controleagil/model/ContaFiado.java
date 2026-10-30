package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Responsabilidade: a "página do caderno" de um cliente: o limite de crédito e o histórico
 * de compras e pagamentos no fiado.
 *
 * Conceito de POO: ENCAPSULAMENTO, o exemplo principal do projeto.
 *  - Não existe setSaldo(). O saldo devedor nem é guardado: é CALCULADO a partir do histórico
 *    (getSaldoDevedor). Assim o saldo nunca fica diferente do que está no caderno.
 *  - O histórico só cresce por registrarCompra(), registrarPagamento() e registrarEstorno(), que
 *    validam os valores (e a RN04) ANTES de anotar qualquer coisa.
 *  - RN03 (limite): a conta SABE dizer se uma compra passa do limite (podeComprar), mas não
 *    recusa a compra. Quem decide é a pessoa no balcão: a tela avisa e pede confirmação.
 *  - getMovimentacoes() devolve uma lista que não pode ser alterada de fora.
 */
public class ContaFiado {

    private BigDecimal limiteCredito;
    private final List<MovimentacaoFiado> movimentacoes = new ArrayList<>();

    /** @param limiteCredito quanto o cliente pode ficar devendo; zero ou mais */
    public ContaFiado(BigDecimal limiteCredito) {
        if (limiteCredito == null || limiteCredito.signum() < 0) {
            throw new RegraNegocioException("O limite de crédito não pode ser negativo.");
        }
        this.limiteCredito = Dinheiro.arredondar(limiteCredito);
    }

    /**
     * Anota uma compra fiado (a dívida aumenta).
     *
     * RN03: a compra pode passar do limite de crédito. O sistema só AVISA (a tela confere com
     * podeComprar() e pede confirmação antes); depois disso, a dívida fica acima do limite e
     * getValorAcimaDoLimite() mostra quanto passou.
     *
     * @param valor  valor da compra, maior que zero
     * @param pedido pedido que originou a compra (pode ser null)
     */
    public void registrarCompra(BigDecimal valor, Pedido pedido) {
        BigDecimal compra = exigirValorPositivo(valor, "O valor da compra precisa ser maior que zero.");
        movimentacoes.add(new MovimentacaoFiado(compra, TipoMovimentacao.COMPRA, pedido));
    }

    /**
     * Anota um pagamento da dívida (a dívida diminui). Pode ser total ou parcial.
     *
     * @param valor valor pago, maior que zero e no máximo o saldo devedor
     */
    public void registrarPagamento(BigDecimal valor) {
        // RN04: o pagamento precisa ser maior que zero...
        BigDecimal pagamento = exigirValorPositivo(valor, "O valor do pagamento precisa ser maior que zero.");
        BigDecimal saldo = getSaldoDevedor();
        if (saldo.signum() == 0) {
            throw new RegraNegocioException("Este cliente não está devendo nada no fiado.");
        }
        // ...e não pode ser maior que o saldo devedor.
        if (pagamento.compareTo(saldo) > 0) {
            throw new RegraNegocioException("O pagamento de " + Dinheiro.formatar(pagamento)
                    + " é maior que o saldo devedor de " + Dinheiro.formatar(saldo) + ".");
        }
        movimentacoes.add(new MovimentacaoFiado(pagamento, TipoMovimentacao.PAGAMENTO, null));
    }

    /**
     * Desfaz (estorna) a compra de um pedido que foi cancelado: a dívida diminui.
     * Se o cliente já pagou parte da dívida, só dá para estornar até o saldo devedor; o que passar
     * disso o cliente já pagou e precisa ser devolvido por fora do sistema.
     *
     * @return quanto foi estornado (pode ser zero, se o cliente não deve nada)
     */
    public BigDecimal registrarEstorno(BigDecimal valor, Pedido pedido) {
        BigDecimal estorno = exigirValorPositivo(valor, "O valor do estorno precisa ser maior que zero.")
                .min(getSaldoDevedor());
        if (estorno.signum() > 0) {
            movimentacoes.add(new MovimentacaoFiado(estorno, TipoMovimentacao.ESTORNO, pedido));
        }
        return estorno;
    }

    /**
     * REMONTA uma linha do histórico que já estava salva no banco. Só o repositório usa este método.
     *
     * Atenção: aqui a RN04 NÃO é conferida de novo, porque já foi conferida no dia em que o
     * pagamento aconteceu. Para uma compra ou um pagamento NOVO, use sempre registrarCompra()
     * e registrarPagamento().
     */
    public void restaurarMovimentacao(TipoMovimentacao tipo, BigDecimal valor, LocalDateTime data, Pedido pedido) {
        movimentacoes.add(new MovimentacaoFiado(Dinheiro.arredondar(valor), tipo, pedido, data));
    }

    /**
     * Quanto o cliente deve agora: soma das compras menos soma dos pagamentos.
     * É o atributo derivado "/saldoDevedor" do modelo: calculado, nunca guardado.
     */
    public BigDecimal getSaldoDevedor() {
        BigDecimal saldo = Dinheiro.ZERO;
        for (MovimentacaoFiado movimentacao : movimentacoes) {
            if (movimentacao.getTipo() == TipoMovimentacao.COMPRA) {
                saldo = saldo.add(movimentacao.getValor());
            } else { // PAGAMENTO ou ESTORNO: a dívida diminui
                saldo = saldo.subtract(movimentacao.getValor());
            }
        }
        return saldo;
    }

    /** Quanto ainda dá para comprar fiado sem passar do limite: limite − saldo devedor (nunca menos que zero). */
    public BigDecimal getCreditoDisponivel() {
        return limiteCredito.subtract(getSaldoDevedor()).max(Dinheiro.ZERO);
    }

    /** Quanto a dívida passou do limite (zero quando está dentro do limite). */
    public BigDecimal getValorAcimaDoLimite() {
        return getSaldoDevedor().subtract(limiteCredito).max(Dinheiro.ZERO);
    }

    /** Se esta compra fosse feita, quanto a dívida ficaria acima do limite (zero quando cabe). */
    public BigDecimal quantoPassariaDoLimite(BigDecimal compra) {
        return getSaldoDevedor().add(compra).subtract(limiteCredito).max(Dinheiro.ZERO);
    }

    /** true quando a dívida já passou do limite. */
    public boolean estaAcimaDoLimite() {
        return getValorAcimaDoLimite().signum() > 0;
    }

    /** Diz se uma compra deste valor cabe no crédito disponível (RN03: se não couber, a tela avisa). */
    public boolean podeComprar(BigDecimal valor) {
        // compareTo, e não equals: para o BigDecimal, 5.0 e 5.00 são "diferentes" no equals.
        return valor != null && valor.compareTo(getCreditoDisponivel()) <= 0;
    }

    /**
     * Muda o limite de crédito (ex.: o cliente ganhou mais crédito, ou o limite foi digitado errado).
     * Pode ficar MENOR que a dívida atual: nesse caso o histórico não muda, a conta passa a
     * aparecer acima do limite e cada nova compra fiado gera o aviso da RN03.
     *
     * @param novoLimite zero ou mais
     */
    public void alterarLimite(BigDecimal novoLimite) {
        if (novoLimite == null || novoLimite.signum() < 0) {
            throw new RegraNegocioException("O limite de crédito não pode ser negativo.");
        }
        this.limiteCredito = Dinheiro.arredondar(novoLimite);
    }

    /** Quanto o cliente pode ficar devendo no máximo. */
    public BigDecimal getLimiteCredito() {
        return limiteCredito;
    }

    /** O histórico, do mais antigo para o mais novo. A lista é somente leitura. */
    public List<MovimentacaoFiado> getMovimentacoes() {
        return Collections.unmodifiableList(movimentacoes);
    }

    /** Arredonda para 2 casas e só depois confere se é maior que zero (0,004 vira 0,00 e é recusado). */
    private static BigDecimal exigirValorPositivo(BigDecimal valor, String mensagem) {
        return Dinheiro.positivo(valor, mensagem);
    }
}
