package br.com.controleagil.model;

import java.math.BigDecimal;

/** Pagamento no fiado: lança o valor como compra na conta fiado do cliente do pedido. */
public class PagamentoFiado extends Pagamento {

    public PagamentoFiado(BigDecimal valor) {
        super(valor);
    }

    @Override
    public void processar(Pedido pedido) {
        Cliente cliente = pedido.getCliente();
        if (!cliente.temContaFiado()) {
            throw new IllegalStateException("O cliente " + cliente.getNome() + " não tem conta fiado.");
        }
        // A própria ContaFiado recusa a compra se passar do limite de crédito.
        cliente.getContaFiado().registrarCompra(getValor(), pedido);
    }
}
