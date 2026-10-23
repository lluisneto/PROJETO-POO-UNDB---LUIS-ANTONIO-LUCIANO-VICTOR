package br.com.controleagil.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pedido agendado: nenhum pedido sem dia e hora de entrega. */
public class Pedido {

    private Long id;
    private final Cliente cliente;
    private final List<ItemPedido> itens = new ArrayList<>();
    private final List<Pagamento> pagamentos = new ArrayList<>();
    private final LocalDateTime dataCriacao;
    private final LocalDateTime dataHoraEntrega;
    private StatusPedido status;

    public Pedido(Cliente cliente, LocalDateTime dataHoraEntrega) {
        if (cliente == null) {
            throw new IllegalArgumentException("O pedido precisa de um cliente.");
        }
        if (dataHoraEntrega == null) {
            throw new IllegalArgumentException("Todo pedido precisa de data e hora de entrega.");
        }
        this.cliente = cliente;
        this.dataHoraEntrega = dataHoraEntrega;
        this.dataCriacao = LocalDateTime.now();
        this.status = StatusPedido.PENDENTE;
    }

    public void adicionarItem(Produto produto, int quantidade) {
        exigirStatusPendente();
        itens.add(new ItemPedido(produto, quantidade));
    }

    public void removerItem(ItemPedido item) {
        exigirStatusPendente();
        if (!itens.remove(item)) {
            throw new IllegalArgumentException("Este item não faz parte do pedido.");
        }
    }

    public BigDecimal calcularTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            total = total.add(item.calcularSubtotal());
        }
        return total;
    }

    /**
     * Polimorfismo: o pedido não sabe se é dinheiro, Pix ou fiado.
     * Ele só chama processar() e cada tipo de Pagamento faz o seu trabalho.
     */
    public void registrarPagamento(Pagamento pagamento) {
        if (pagamento == null) {
            throw new IllegalArgumentException("Informe o pagamento.");
        }
        if (status == StatusPedido.CANCELADO) {
            throw new IllegalStateException("Não é possível pagar um pedido cancelado.");
        }
        BigDecimal faltaPagar = calcularTotal().subtract(calcularValorPago());
        if (pagamento.getValor().compareTo(faltaPagar) > 0) {
            throw new IllegalArgumentException("O pagamento de R$ " + pagamento.getValor()
                    + " é maior que o valor que falta pagar (R$ " + faltaPagar + ").");
        }
        pagamento.processar(this);
        pagamentos.add(pagamento);
    }

    public BigDecimal calcularValorPago() {
        BigDecimal pago = BigDecimal.ZERO;
        for (Pagamento pagamento : pagamentos) {
            pago = pago.add(pagamento.getValor());
        }
        return pago;
    }

    /**
     * O status só anda para frente (pode pular etapas, ex.: retirada no local vai direto
     * para ENTREGUE). CANCELADO pode vir de qualquer etapa que ainda não seja final.
     */
    public void alterarStatus(StatusPedido novoStatus) {
        if (novoStatus == null) {
            throw new IllegalArgumentException("Informe o novo status.");
        }
        if (status.isFinal()) {
            throw new IllegalStateException("O pedido já está " + status + " e não pode mudar de status.");
        }
        if (novoStatus != StatusPedido.CANCELADO && novoStatus.ordinal() <= status.ordinal()) {
            throw new IllegalStateException("O pedido não pode voltar de " + status + " para " + novoStatus + ".");
        }
        this.status = novoStatus;
    }

    /** Usado no alerta de entregas próximas. Pedidos já atrasados também entram no alerta. */
    public boolean estaProximoDaEntrega(int minutos) {
        if (status.isFinal()) {
            return false;
        }
        return !LocalDateTime.now().isBefore(dataHoraEntrega.minusMinutes(minutos));
    }

    public Long getId() {
        return id;
    }

    /** Usado pelo repository depois de salvar no banco. */
    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public List<Pagamento> getPagamentos() {
        return Collections.unmodifiableList(pagamentos);
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataHoraEntrega() {
        return dataHoraEntrega;
    }

    public StatusPedido getStatus() {
        return status;
    }

    private void exigirStatusPendente() {
        if (status != StatusPedido.PENDENTE) {
            throw new IllegalStateException("Os itens só podem ser alterados enquanto o pedido está PENDENTE.");
        }
    }
}
