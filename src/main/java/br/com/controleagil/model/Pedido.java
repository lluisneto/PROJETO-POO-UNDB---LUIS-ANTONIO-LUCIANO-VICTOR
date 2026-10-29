package br.com.controleagil.model;

import br.com.controleagil.exception.RegraNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Responsabilidade: um pedido agendado de um cliente: itens, pagamentos, dia e hora de entrega
 * e status. É a classe central do módulo de pedidos.
 *
 * Conceitos de POO:
 *  - COMPOSIÇÃO: os ItemPedido e os Pagamento vivem DENTRO do pedido (losango preto no diagrama).
 *  - ASSOCIAÇÃO: o pedido conhece o seu Cliente, mas o cliente existe sem o pedido.
 *  - POLIMORFISMO: em registrarPagamento(), o pedido chama pagamento.processar(this) sem saber
 *    se é dinheiro, Pix ou fiado. Cada tipo de pagamento faz o seu trabalho.
 *  - ENCAPSULAMENTO: listas privadas, entregues para fora só como somente leitura; status só
 *    muda por alterarStatus(), que aplica a RN05.
 */
public class Pedido {

    private Long id;
    private final Cliente cliente;
    private final List<ItemPedido> itens = new ArrayList<>();
    private final List<Pagamento> pagamentos = new ArrayList<>();
    private final LocalDateTime dataCriacao;
    private final LocalDateTime dataHoraEntrega;
    private StatusPedido status;

    /** Cria um pedido novo, anotado agora. Todo pedido nasce PENDENTE. */
    public Pedido(Cliente cliente, LocalDateTime dataHoraEntrega) {
        this(cliente, dataHoraEntrega, LocalDateTime.now());
    }

    /**
     * Cria um pedido informando quando ele foi anotado. Útil para quem controla o relógio
     * (o PedidoService) e, no futuro, para carregar pedidos do banco.
     */
    public Pedido(Cliente cliente, LocalDateTime dataHoraEntrega, LocalDateTime dataCriacao) {
        if (cliente == null) {
            throw new RegraNegocioException("Escolha o cliente do pedido.");
        }
        // RN01: todo pedido precisa de data e hora de entrega.
        if (dataHoraEntrega == null) {
            throw new RegraNegocioException("Todo pedido precisa de data e hora de entrega.");
        }
        this.cliente = cliente;
        this.dataHoraEntrega = dataHoraEntrega;
        this.dataCriacao = dataCriacao == null ? LocalDateTime.now() : dataCriacao;
        this.status = StatusPedido.PENDENTE;
    }

    /**
     * Adiciona um produto ao pedido, copiando o preço de agora (preço da época).
     * Se o produto já estiver no pedido, soma a quantidade na mesma linha.
     */
    public void adicionarItem(Produto produto, int quantidade) {
        exigirStatusPendente();
        for (int i = 0; i < itens.size(); i++) {
            ItemPedido existente = itens.get(i);
            if (mesmoProduto(existente.getProduto(), produto)) {
                itens.set(i, existente.somarQuantidade(quantidade));
                return;
            }
        }
        itens.add(new ItemPedido(produto, quantidade));
    }

    /** Tira um item do pedido. */
    public void removerItem(ItemPedido item) {
        exigirStatusPendente();
        if (!itens.remove(item)) {
            throw new RegraNegocioException("Este item não faz parte do pedido.");
        }
    }

    /** Valor total: soma dos subtotais dos itens. É o atributo derivado "/valorTotal": calculado, nunca guardado. */
    public BigDecimal calcularTotal() {
        BigDecimal total = Dinheiro.ZERO;
        for (ItemPedido item : itens) {
            total = total.add(item.calcularSubtotal());
        }
        return total;
    }

    /**
     * Registra um pagamento (total ou parcial) no pedido.
     *
     * POLIMORFISMO: a variável "pagamento" é do tipo Pagamento (a classe abstrata), mas o objeto
     * de verdade é um PagamentoDinheiro, PagamentoPix ou PagamentoFiado. Na hora de rodar, o Java
     * chama o processar() da classe de verdade. O pedido não precisa de nenhum "if" de tipo.
     */
    public void registrarPagamento(Pagamento pagamento) {
        if (pagamento == null) {
            throw new RegraNegocioException("Informe o pagamento.");
        }
        if (status == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Não dá para registrar pagamento de um pedido cancelado.");
        }
        // A soma dos pagamentos não pode passar do total do pedido.
        BigDecimal faltaPagar = calcularTotal().subtract(calcularValorPago());
        if (pagamento.getValor().compareTo(faltaPagar) > 0) {
            throw new RegraNegocioException("O pagamento de " + Dinheiro.formatar(pagamento.getValor())
                    + " passa do que falta pagar neste pedido (" + Dinheiro.formatar(faltaPagar) + ").");
        }
        pagamento.processar(this); // se o pagamento não puder ser feito, processar() lança a exceção
        pagamentos.add(pagamento); // e esta linha nem chega a rodar
    }

    /** Quanto já foi pago: soma dos pagamentos registrados. */
    public BigDecimal calcularValorPago() {
        BigDecimal pago = Dinheiro.ZERO;
        for (Pagamento pagamento : pagamentos) {
            pago = pago.add(pagamento.getValor());
        }
        return pago;
    }

    /**
     * Muda o status do pedido.
     * O status só avança UM passo por vez, na ordem (veja StatusPedido.proximo()).
     * CANCELADO pode vir de qualquer status que ainda não seja final.
     */
    public void alterarStatus(StatusPedido novoStatus) {
        if (novoStatus == null) {
            throw new RegraNegocioException("Informe o novo status.");
        }
        // RN05: pedido ENTREGUE ou CANCELADO não muda mais de status.
        if (status.isFinal()) {
            throw new RegraNegocioException(descricaoCurta() + " já está " + status.getDescricao().toLowerCase()
                    + " e não muda mais de status.");
        }
        if (novoStatus != StatusPedido.CANCELADO && novoStatus != status.proximo()) {
            throw new RegraNegocioException(descricaoCurta() + " está \"" + status.getDescricao()
                    + "\" e só pode avançar para \"" + status.proximo().getDescricao() + "\" (ou ser cancelado).");
        }
        this.status = novoStatus;
    }

    /**
     * Diz se o pedido ainda não foi finalizado e a entrega está a "minutos" minutos ou menos de agora.
     * Pedidos já atrasados também contam como próximos (precisam de atenção).
     */
    public boolean estaProximoDaEntrega(int minutos) {
        return estaProximoDaEntrega(minutos, LocalDateTime.now());
    }

    /** Mesma regra, mas recebendo o "agora". Assim os testes e o service controlam o relógio. */
    public boolean estaProximoDaEntrega(int minutos, LocalDateTime agora) {
        if (status.isFinal()) {
            return false;
        }
        return !agora.isBefore(dataHoraEntrega.minusMinutes(minutos));
    }

    public Long getId() {
        return id;
    }

    /** Chamado só pelo repositório: o id é o número do pedido. */
    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    /** Itens do pedido, somente leitura. Para mudar, use adicionarItem() e removerItem(). */
    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
    }

    /** Pagamentos do pedido, somente leitura. Para pagar, use registrarPagamento(). */
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
            throw new RegraNegocioException("Os itens só podem mudar enquanto o pedido está pendente.");
        }
    }

    /** "Pedido nº 12" (ou só "O pedido", se ainda não foi salvo e não tem número). */
    private String descricaoCurta() {
        return id == null ? "O pedido" : "O pedido nº " + id;
    }

    /** Mesmo objeto, ou dois objetos com o mesmo id (caso venham do banco). */
    private static boolean mesmoProduto(Produto a, Produto b) {
        if (a == b) {
            return true;
        }
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }
}
