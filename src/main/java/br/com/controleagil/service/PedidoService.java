package br.com.controleagil.service;

import br.com.controleagil.exception.RegraNegocioException;
import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.ItemPedido;
import br.com.controleagil.model.Pagamento;
import br.com.controleagil.model.PagamentoDinheiro;
import br.com.controleagil.model.PagamentoCartao;
import br.com.controleagil.model.PagamentoFiado;
import br.com.controleagil.model.PagamentoPix;
import br.com.controleagil.model.Pedido;
import br.com.controleagil.model.Produto;
import br.com.controleagil.model.StatusPedido;
import br.com.controleagil.model.TipoCartao;
import br.com.controleagil.repository.PedidoRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/*
 * Responsabilidade: os casos de uso de pedidos: registrar, receber pagamento, mudar status
 * e as consultas da agenda (pedidos do dia, entregas próximas, atrasados).
 *
 * Conceitos:
 *  - CAMADA DE SERVIÇO: regras que envolvem vários objetos ou o repositório (ex.: RN02, que só
 *    faz sentido na hora de salvar) ficam aqui. Regras de um objeto só ficam no próprio objeto.
 *  - INJEÇÃO DE DEPENDÊNCIA: recebe o repositório (interface) e o RELÓGIO no construtor.
 *    Receber o relógio (Clock) em vez de chamar LocalDateTime.now() direto deixa os testes
 *    escolherem "que horas são", e assim dá para testar atraso e data no passado sem esperar.
 */
public class PedidoService {

    /** RF07: com quantos minutos de antecedência o pedido aparece como "Em breve" (validar com o cliente). */
    public static final int MINUTOS_EM_BREVE = 60;

    private final PedidoRepository repositorio;
    private final Clock relogio;

    /**
     * @param repositorio onde os pedidos ficam. Ao salvar um pedido, ele grava junto o que o pedido
     *                    mudou na conta fiado do cliente (compra no fiado, estorno ao cancelar).
     */
    public PedidoService(PedidoRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    /**
     * Um PedidoService igual a este (mesmo repositório), mas com outro relógio.
     * Usado pelos DadosExemplo para anotar, "antes do expediente", pedidos que já foram entregues.
     */
    public PedidoService comRelogio(Clock outroRelogio) {
        return new PedidoService(repositorio, outroRelogio);
    }

    /** Data de hoje, segundo o relógio do service. */
    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    // ------------------------------------------------------------------ registrar pedido

    /**
     * Registra um pedido novo e, se vier, o primeiro pagamento.
     * Se qualquer regra falhar, nada é salvo.
     *
     * @param pagamento pagamento feito na hora; null quando o cliente vai "pagar depois"
     * @return o pedido salvo, já com número (id)
     */
    public Pedido registrarPedido(Cliente cliente, LocalDateTime dataHoraEntrega,
                                  List<ItemPedido> itens, Pagamento pagamento) {
        // RN01 (data e hora obrigatórias) é conferida pelo construtor do Pedido.
        Pedido pedido = new Pedido(cliente, dataHoraEntrega, agora());

        if (dataHoraEntrega.isBefore(agora().truncatedTo(ChronoUnit.MINUTES))) {
            throw new RegraNegocioException("A data e hora de entrega não pode estar no passado.");
        }
        // RN02: pedido precisa de pelo menos um item para ser salvo.
        if (itens == null || itens.isEmpty()) {
            throw new RegraNegocioException("Adicione pelo menos um item ao pedido.");
        }
        for (ItemPedido item : itens) {
            pedido.adicionarItem(item.getProduto(), item.getQuantidade());
        }
        if (pagamento != null) {
            // Polimorfismo: dinheiro, Pix, cartão ou fiado. Cada forma confere os próprios dados.
            pedido.registrarPagamento(pagamento);
        }
        return repositorio.salvar(pedido); // grava o pedido e, se foi no fiado, a compra na conta do cliente
    }

    /**
     * Rascunho da tela "Novo pedido": devolve uma lista NOVA com o produto adicionado.
     * Se o produto já estiver na lista, soma a quantidade na mesma linha.
     */
    public List<ItemPedido> adicionarAoRascunho(List<ItemPedido> itens, Produto produto, int quantidade) {
        List<ItemPedido> resultado = new ArrayList<>(itens);
        for (int i = 0; i < resultado.size(); i++) {
            if (resultado.get(i).getProduto() == produto) {
                resultado.set(i, resultado.get(i).somarQuantidade(quantidade));
                return List.copyOf(resultado);
            }
        }
        resultado.add(new ItemPedido(produto, quantidade)); // o construtor valida produto e quantidade
        return List.copyOf(resultado);
    }

    /** Total de uma lista de itens (usado no rascunho, antes de o pedido existir). */
    public BigDecimal calcularTotal(List<ItemPedido> itens) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (ItemPedido item : itens) {
            total = total.add(item.calcularSubtotal());
        }
        return total;
    }

    // ------------------------------------------------------------------ pagamentos

    /**
     * Cria o objeto de pagamento da forma escolhida na tela.
     * É o ÚNICO lugar que decide qual subclasse de Pagamento criar; depois disso, o resto do
     * sistema trata todos como "Pagamento" (polimorfismo).
     *
     * @param valorRecebido só para dinheiro (pode ser null nas outras formas)
     * @param codigoPix     só para Pix (opcional)
     */
    public Pagamento criarPagamento(FormaPagamento forma, BigDecimal valor, BigDecimal valorRecebido, String codigoPix) {
        if (forma == null) {
            throw new RegraNegocioException("Escolha a forma de pagamento.");
        }
        return switch (forma) {
            case DINHEIRO -> new PagamentoDinheiro(valor, valorRecebido);
            case PIX -> new PagamentoPix(valor, codigoPix);
            case CARTAO_DEBITO -> new PagamentoCartao(valor, TipoCartao.DEBITO);
            case CARTAO_CREDITO -> new PagamentoCartao(valor, TipoCartao.CREDITO);
            case FIADO -> new PagamentoFiado(valor);
        };
    }

    /** Troco do dinheiro. Negativo quer dizer que o valor recebido ainda não cobre o valor a pagar. */
    public BigDecimal calcularTroco(BigDecimal valorAPagar, BigDecimal valorRecebido) {
        return valorRecebido.subtract(valorAPagar);
    }

    /** Quanto ainda falta pagar no pedido: total − pago. */
    public BigDecimal calcularValorRestante(Pedido pedido) {
        return pedido.calcularTotal().subtract(pedido.calcularValorPago());
    }

    /** Registra um pagamento (total ou parcial) num pedido que já existe. */
    public void registrarPagamento(Pedido pedido, Pagamento pagamento) {
        pedido.registrarPagamento(pagamento);
        repositorio.salvar(pedido); // no fiado, a compra na conta do cliente é gravada junto
    }

    // ------------------------------------------------------------------ status

    /** Avança o pedido para o próximo status (ex.: Pendente → Em produção). RN05 vale aqui. */
    public void avancarStatus(Pedido pedido) {
        // RN05: pedido entregue ou cancelado não muda mais de status.
        if (pedido.getStatus().isFinal()) {
            throw new RegraNegocioException("O pedido nº " + pedido.getId() + " já está "
                    + pedido.getStatus().getDescricao().toLowerCase() + " e não muda mais de status.");
        }
        pedido.alterarStatus(pedido.getStatus().proximo());
        repositorio.salvar(pedido);
    }

    /** Cancela o pedido. Só vale para pedido que ainda não foi entregue nem cancelado (RN05). */
    public void cancelar(Pedido pedido) {
        pedido.alterarStatus(StatusPedido.CANCELADO);
        repositorio.salvar(pedido);
    }

    // ------------------------------------------------------------------ consultas

    /** Todos os pedidos, do horário de entrega mais cedo para o mais tarde. */
    public List<Pedido> listarTodos() {
        return repositorio.listarTodos();
    }

    /**
     * RF06: pedidos de um dia, por horário.
     *
     * @param filtro mostra só este status; null mostra todos (inclusive os cancelados)
     */
    public List<Pedido> pedidosDoDia(LocalDate dia, StatusPedido filtro) {
        return repositorio.listarPorDataEntrega(dia).stream()
                .filter(pedido -> filtro == null || pedido.getStatus() == filtro)
                .toList();
    }

    /** Pedidos de um dia que valem para a operação: todos menos os cancelados. */
    public List<Pedido> agendaDoDia(LocalDate dia) {
        return repositorio.listarPorDataEntrega(dia).stream()
                .filter(pedido -> pedido.getStatus() != StatusPedido.CANCELADO)
                .toList();
    }

    /** RF07: pedidos em aberto com entrega nos próximos "minutos" (ou já atrasados), por horário. */
    public List<Pedido> entregasProximas(int minutos) {
        LocalDateTime agora = agora();
        return repositorio.listarTodos().stream()
                .filter(pedido -> pedido.estaProximoDaEntrega(minutos, agora))
                .toList();
    }

    /**
     * Painel "Próximas entregas": primeiro os atrasados de dias anteriores (os mais urgentes),
     * depois os pedidos de hoje ainda não finalizados, por horário. No máximo "limite" pedidos.
     */
    public List<Pedido> proximasEntregas(int limite) {
        List<Pedido> lista = new ArrayList<>(atrasadosDeOutrosDias());
        agendaDoDia(hoje()).stream()
                .filter(pedido -> !pedido.getStatus().isFinal())
                .forEach(lista::add);
        return lista.stream().limit(limite).toList();
    }

    /** Todos os pedidos atrasados (de qualquer dia), do mais antigo para o mais novo. */
    public List<Pedido> atrasados() {
        return repositorio.listarTodos().stream().filter(this::estaAtrasado).toList();
    }

    /**
     * Atrasados de DIAS ANTERIORES: pedidos que ninguém marcou como entregues (ou cancelados).
     * Eles não aparecem na agenda de hoje, por isso as telas avisam que existem.
     */
    public List<Pedido> atrasadosDeOutrosDias() {
        return atrasados().stream()
                .filter(pedido -> pedido.getDataHoraEntrega().toLocalDate().isBefore(hoje()))
                .toList();
    }

    /** Quantos pedidos (sem contar os cancelados) têm entrega hoje. */
    public long contarPedidosDeHoje() {
        return agendaDoDia(hoje()).size();
    }

    /** Quantos pedidos em aberto têm entrega entre agora e daqui a "horas" horas. */
    public long contarEntregasNasProximasHoras(int horas) {
        LocalDateTime agora = agora();
        LocalDateTime limite = agora.plusHours(horas);
        return repositorio.listarTodos().stream()
                .filter(pedido -> !pedido.getStatus().isFinal())
                .filter(pedido -> !pedido.getDataHoraEntrega().isBefore(agora)
                        && !pedido.getDataHoraEntrega().isAfter(limite))
                .count();
    }

    /** Quantos pedidos em aberto já passaram do horário de entrega (de qualquer dia). */
    public long contarAtrasados() {
        return atrasados().size();
    }

    /** "Atrasado": ainda não finalizado e o horário de entrega já passou. */
    public boolean estaAtrasado(Pedido pedido) {
        return !pedido.getStatus().isFinal() && pedido.getDataHoraEntrega().isBefore(agora());
    }

    /** "Em breve": ainda não finalizado, não atrasado e com entrega nos próximos MINUTOS_EM_BREVE minutos. */
    public boolean estaEmBreve(Pedido pedido) {
        return !estaAtrasado(pedido) && pedido.estaProximoDaEntrega(MINUTOS_EM_BREVE, agora());
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }
}
