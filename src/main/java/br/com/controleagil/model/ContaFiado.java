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
 *  - O histórico só cresce por registrarCompra() e registrarPagamento(), que validam as regras
 *    RN03 e RN04 ANTES de anotar qualquer coisa.
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
     * @param valor  valor da compra, maior que zero
     * @param pedido pedido que originou a compra (pode ser null)
     */
    public void registrarCompra(BigDecimal valor, Pedido pedido) {
        BigDecimal compra = exigirValorPositivo(valor, "O valor da compra precisa ser maior que zero.");
        // RN03: compra fiado não pode passar do limite de crédito. Neste esqueleto, BLOQUEIA a compra.
        if (!podeComprar(compra)) {
            throw new RegraNegocioException("Compra fiado de " + Dinheiro.formatar(compra)
                    + " passa do limite. Crédito disponível: " + Dinheiro.formatar(getCreditoDisponivel())
                    + " (limite de " + Dinheiro.formatar(limiteCredito) + ").");
        }
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
     * Atenção: aqui as regras RN03 e RN04 NÃO são conferidas de novo, porque já foram conferidas no
     * dia em que a compra ou o pagamento aconteceu (e o limite pode ter mudado depois). Para uma
     * compra ou um pagamento NOVO, use sempre registrarCompra() e registrarPagamento().
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

    /** Quanto ainda dá para comprar fiado: limite − saldo devedor. */
    public BigDecimal getCreditoDisponivel() {
        return limiteCredito.subtract(getSaldoDevedor());
    }

    /** Diz se uma compra deste valor cabe no crédito disponível (usado pela RN03). */
    public boolean podeComprar(BigDecimal valor) {
        // compareTo, e não equals: para o BigDecimal, 5.0 e 5.00 são "diferentes" no equals.
        return valor != null && valor.compareTo(getCreditoDisponivel()) <= 0;
    }

    /**
     * Muda o limite de crédito (ex.: o cliente ganhou mais crédito, ou o limite foi digitado errado).
     * Pode ficar MENOR que a dívida atual: nesse caso o histórico não muda, mas o cliente não
     * consegue comprar mais fiado até pagar (RN03).
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
