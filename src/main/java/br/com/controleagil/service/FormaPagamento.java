package br.com.controleagil.service;

/*
 * Responsabilidade: listar as formas de pagamento que a TELA oferece (os botões de opção).
 *
 * Por que existe, se no model já há PagamentoDinheiro, PagamentoPix e PagamentoFiado?
 * No model, cada forma é uma CLASSE (para usar polimorfismo). Mas a tela precisa de uma lista
 * simples para mostrar as opções. O PedidoService.criarPagamento() faz a ponte: recebe a opção
 * escolhida e cria o objeto da subclasse certa. (No modelo de domínio, isto era a enumeração
 * FormaPagamento; no diagrama de classes ela virou herança.)
 */
public enum FormaPagamento {

    DINHEIRO("Dinheiro"),
    PIX("Pix"),
    FIADO("Fiado");

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
