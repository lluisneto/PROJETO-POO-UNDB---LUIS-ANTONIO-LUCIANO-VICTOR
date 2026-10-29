package br.com.controleagil.service;

import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.ItemPedido;
import br.com.controleagil.model.Pagamento;
import br.com.controleagil.model.Pedido;
import br.com.controleagil.model.Produto;
import br.com.controleagil.model.StatusPedido;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/*
 * ============================================================================================
 *   DADOS FICTÍCIOS, SÓ PARA DEMONSTRAÇÃO. SUBSTITUIR PELOS DADOS REAIS DO CLIENTE.
 *   Os nomes, telefones, endereços e PREÇOS abaixo são inventados.
 *   Os preços NÃO são os preços da A L Salgados Distribuidora.
 * ============================================================================================
 *
 * Responsabilidade: encher o sistema com produtos, clientes e pedidos de exemplo quando ele abre,
 * para a interface já aparecer "viva" (com agenda, atrasados, fiado...).
 *
 * Tudo passa pelos SERVICES, como se alguém estivesse usando o sistema: nenhum atalho.
 * Assim os pedidos fiado geram as movimentações na conta respeitando o limite (RN03), e um dado
 * de exemplo que quebre uma regra faz o sistema avisar logo na abertura.
 *
 * Os horários são calculados a partir da hora em que o sistema abre, para sempre existir
 * um pedido atrasado, um "em breve" e outros mais tarde.
 */
public final class DadosExemplo {

    /** Faixa de horário em que a distribuidora entrega. Horários calculados fora dela são ajustados. */
    private static final LocalTime PRIMEIRA_ENTREGA = LocalTime.of(6, 0);
    private static final LocalTime ULTIMA_ENTREGA = LocalTime.of(22, 0);

    private DadosExemplo() {
        // Só tem o método estático carregar().
    }

    /** Carrega os dados de exemplo usando os services (as regras de negócio valem para eles também). */
    public static void carregar(ClienteService clientes, ProdutoService produtos,
                                PedidoService pedidos, FiadoService fiado, Clock relogio) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDate hoje = agora.toLocalDate();

        // ----- 8 produtos (preços fictícios) -----
        Produto coxinha = produtos.cadastrar("Coxinha de frango", reais("5.00"));
        Produto kibe = produtos.cadastrar("Kibe", reais("5.00"));
        Produto bolinha = produtos.cadastrar("Bolinha de queijo", reais("4.50"));
        Produto enroladinho = produtos.cadastrar("Enroladinho de salsicha", reais("4.50"));
        Produto empada = produtos.cadastrar("Empada de frango", reais("6.00"));
        Produto esfirra = produtos.cadastrar("Esfirra de carne", reais("5.50"));
        Produto pastel = produtos.cadastrar("Pastel de forno", reais("6.50"));
        Produto miniPizza = produtos.cadastrar("Mini pizza", reais("7.00"));

        // ----- 6 clientes fictícios (comércios e pessoas) -----
        Cliente lanchonete = clientes.cadastrar("Lanchonete Sabor da Praça", "(98) 99000-0101", "Rua da Paz, 120, Centro");
        Cliente mercadinho = clientes.cadastrar("Mercadinho Bom Preço", "(98) 99000-0102", "Av. Principal, 45, Cohama");
        Cliente cantina = clientes.cadastrar("Cantina Pequeno Saber", "(98) 99000-0103", "Rua das Flores, 300, Turu");
        Cliente padaria = clientes.cadastrar("Padaria Pão Quente", "(98) 99000-0104", "Rua do Sol, 78, Renascença");
        Cliente ana = clientes.cadastrar("Ana Beatriz Souza", "(98) 99000-0105", "Rua 10, Quadra 5, Casa 12, Cohab");
        Cliente carlos = clientes.cadastrar("Carlos Henrique Lima", "(98) 99000-0106", "Av. Litorânea, 1500, Calhau");

        // ----- 3 contas fiado: saldo baixo, perto do limite e sem dívida (os saldos vêm dos pedidos) -----
        clientes.abrirContaFiado(lanchonete, reais("300.00"));
        clientes.abrirContaFiado(mercadinho, reais("500.00"));
        clientes.abrirContaFiado(cantina, reais("400.00"));

        // Os pedidos de hoje foram "anotados de manhã cedo", antes de qualquer entrega.
        // Por isso usamos um PedidoService com o relógio parado às 05:30: assim a regra
        // "entrega não pode estar no passado" continua valendo para eles também.
        Clock madrugada = Clock.fixed(hoje.atTime(5, 30).atZone(relogio.getZone()).toInstant(), relogio.getZone());
        PedidoService anotados = pedidos.comRelogio(madrugada);

        // 2 já entregues hoje: um pago em dinheiro (com troco) e um no fiado.
        Pedido entregueDinheiro = registrar(anotados, ana, horario(agora.minusHours(3), hoje),
                List.of(new ItemPedido(coxinha, 10), new ItemPedido(kibe, 10)),
                FormaPagamento.DINHEIRO, reais("120.00"), null);
        avancarAte(anotados, entregueDinheiro, StatusPedido.ENTREGUE);

        Pedido entregueFiado = registrar(anotados, lanchonete, horario(agora.minusHours(2), hoje),
                List.of(new ItemPedido(esfirra, 8)), FormaPagamento.FIADO, null, null);
        avancarAte(anotados, entregueFiado, StatusPedido.ENTREGUE);
        fiado.registrarPagamento(lanchonete, reais("14.00")); // pagou uma parte: saldo baixo (R$ 30,00)

        // 1 atrasado (ainda em produção, sem pagamento).
        Pedido atrasado = registrar(anotados, carlos, horario(agora.minusMinutes(40), hoje),
                List.of(new ItemPedido(pastel, 6), new ItemPedido(miniPizza, 6)), null, null, null);
        avancarAte(anotados, atrasado, StatusPedido.EM_PRODUCAO);

        // 1 "em breve" (daqui a ~30 min), já pago no Pix.
        Pedido emBreve = registrar(anotados, padaria, horario(agora.plusMinutes(30), hoje),
                List.of(new ItemPedido(coxinha, 30), new ItemPedido(bolinha, 20)),
                FormaPagamento.PIX, null, "E2E20261029PADARIA0001");
        avancarAte(anotados, emBreve, StatusPedido.SAIU_PARA_ENTREGA);

        // Mais tarde hoje, em status variados.
        // Mercadinho: R$ 455,00 no fiado com limite de R$ 500,00 = perto do limite.
        registrar(anotados, mercadinho, horario(agora.plusHours(2), hoje),
                List.of(new ItemPedido(coxinha, 40), new ItemPedido(enroladinho, 30), new ItemPedido(empada, 20)),
                FormaPagamento.FIADO, null, null);

        // Cantina: comprou no fiado e já quitou: conta sem dívida, mas com histórico.
        Pedido cantinaHoje = registrar(anotados, cantina, horario(agora.plusMinutes(210), hoje),
                List.of(new ItemPedido(miniPizza, 20), new ItemPedido(bolinha, 20)), FormaPagamento.FIADO, null, null);
        avancarAte(anotados, cantinaHoje, StatusPedido.EM_PRODUCAO);
        fiado.registrarPagamento(cantina, cantinaHoje.calcularTotal());

        registrar(anotados, ana, horario(agora.plusHours(5), hoje),
                List.of(new ItemPedido(empada, 12)), null, null, null);

        Pedido cancelado = registrar(anotados, lanchonete, horario(agora.plusHours(6), hoje),
                List.of(new ItemPedido(pastel, 15)), null, null, null);
        anotados.cancelar(cancelado); // o cliente desmarcou

        // 2 para amanhã, ainda sem pagamento.
        registrar(anotados, padaria, hoje.plusDays(1).atTime(9, 0),
                List.of(new ItemPedido(coxinha, 50)), null, null, null);
        registrar(anotados, cantina, hoje.plusDays(1).atTime(14, 30),
                List.of(new ItemPedido(kibe, 30), new ItemPedido(bolinha, 30)), null, null, null);
    }

    /**
     * Registra um pedido pelo service. Se "forma" vier preenchida, já paga o valor total
     * nessa forma; se vier null, o pedido fica para "pagar depois".
     */
    private static Pedido registrar(PedidoService service, Cliente cliente, LocalDateTime entrega,
                                    List<ItemPedido> itens, FormaPagamento forma,
                                    BigDecimal valorRecebido, String codigoPix) {
        Pagamento pagamento = forma == null ? null
                : service.criarPagamento(forma, service.calcularTotal(itens), valorRecebido, codigoPix);
        return service.registrarPedido(cliente, entrega, itens, pagamento);
    }

    /** Avança o status passo a passo (a regra não deixa pular etapas) até chegar no status desejado. */
    private static void avancarAte(PedidoService service, Pedido pedido, StatusPedido destino) {
        while (pedido.getStatus() != destino) {
            service.avancarStatus(pedido);
        }
    }

    /**
     * Ajusta um horário calculado para ficar plausível: dentro do dia de hoje, entre 06:00 e 22:00,
     * e "redondo" de 5 em 5 minutos (ninguém marca entrega para 14:37).
     */
    private static LocalDateTime horario(LocalDateTime calculado, LocalDate hoje) {
        LocalDateTime primeira = hoje.atTime(PRIMEIRA_ENTREGA);
        LocalDateTime ultima = hoje.atTime(ULTIMA_ENTREGA);
        LocalDateTime ajustado = calculado;
        if (ajustado.isBefore(primeira)) {
            ajustado = primeira;
        } else if (ajustado.isAfter(ultima)) {
            ajustado = ultima;
        }
        return ajustado.withSecond(0).withNano(0).minusMinutes(ajustado.getMinute() % 5);
    }

    private static BigDecimal reais(String valor) {
        return new BigDecimal(valor);
    }
}
