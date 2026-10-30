package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * Responsabilidade: "pagar" deixando na conta: lança o valor como COMPRA na conta fiado do cliente.
 *
 * Conceitos de POO: HERANÇA ("extends Pagamento") e POLIMORFISMO. Este é o processar() mais
 * diferente de todos: em vez de só conferir dados, ele mexe em outro objeto (a ContaFiado).
 * Repare que o limite (RN03) NÃO é conferido aqui: quem conhece o limite é a própria ContaFiado
 * (podeComprar), e passar do limite só gera um aviso na tela. Cada objeto cuida das suas regras.
 */
public class PagamentoFiado extends Pagamento {

    public PagamentoFiado(BigDecimal valor) {
        super(valor);
    }

    /**
     * Usado ao remontar um pagamento salvo no banco. Remontar NÃO chama processar(): a compra já
     * está no histórico da conta fiado, e lançar de novo dobraria a dívida.
     */
    public PagamentoFiado(BigDecimal valor, LocalDateTime data) {
        super(valor, data);
    }

    /** Fiado: o cliente precisa ter conta fiado. A compra entra na conta mesmo se passar do limite (RN03 só avisa). */
    @Override
    public void processar(Pedido pedido) {
        Cliente cliente = pedido.getCliente();
        if (!cliente.temContaFiado()) {
            throw new RegraNegocioException("O cliente " + cliente.getNome()
                    + " não tem conta fiado. Abra a conta na tela Clientes.");
        }
        cliente.getContaFiado().registrarCompra(getValor(), pedido);
    }

    /** Pedido cancelado: a compra lançada no fiado é estornada (sai da dívida do cliente). */
    @Override
    public void cancelar(Pedido pedido) {
        Cliente cliente = pedido.getCliente();
        if (cliente.temContaFiado()) {
            cliente.getContaFiado().registrarEstorno(getValor(), pedido);
        }
    }

    /** Sempre "Fiado". */
    @Override
    public String getDescricao() {
        return "Fiado";
    }
}
