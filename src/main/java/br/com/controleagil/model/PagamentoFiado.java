package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;

import java.math.BigDecimal;

/*
 * Responsabilidade: "pagar" deixando na conta: lança o valor como COMPRA na conta fiado do cliente.
 *
 * Conceitos de POO: HERANÇA ("extends Pagamento") e POLIMORFISMO. Este é o processar() mais
 * diferente dos três: em vez de só conferir dados, ele mexe em outro objeto (a ContaFiado).
 * Repare que a regra do limite (RN03) NÃO está aqui: ela fica na própria ContaFiado,
 * que é quem conhece o limite. Cada objeto cuida das suas regras.
 */
public class PagamentoFiado extends Pagamento {

    public PagamentoFiado(BigDecimal valor) {
        super(valor);
    }

    /** Fiado: o cliente precisa ter conta fiado, e a compra precisa caber no limite (RN03). */
    @Override
    public void processar(Pedido pedido) {
        Cliente cliente = pedido.getCliente();
        if (!cliente.temContaFiado()) {
            throw new RegraNegocioException("O cliente " + cliente.getNome()
                    + " não tem conta fiado. Abra a conta na tela Clientes.");
        }
        cliente.getContaFiado().registrarCompra(getValor(), pedido);
    }

    @Override
    public String getDescricao() {
        return "Fiado";
    }
}
