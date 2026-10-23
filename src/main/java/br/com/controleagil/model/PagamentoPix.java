package br.com.controleagil.model;

import java.math.BigDecimal;

/** Pagamento via Pix: só é aceito com o código da transação. */
public class PagamentoPix extends Pagamento {

    private final String codigoTransacao;

    public PagamentoPix(BigDecimal valor, String codigoTransacao) {
        super(valor);
        this.codigoTransacao = codigoTransacao;
    }

    @Override
    public void processar(Pedido pedido) {
        if (codigoTransacao == null || codigoTransacao.isBlank()) {
            throw new IllegalStateException("Informe o código da transação Pix para confirmar o pagamento.");
        }
    }

    public String getCodigoTransacao() {
        return codigoTransacao;
    }
}
