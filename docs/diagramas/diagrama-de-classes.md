# Diagrama de classes: Controle Ágil (Etapa 1)

Modelo de domínio do MVP (pedidos agendados + conta fiado). O código fica em
[`src/main/java/br/com/controleagil/model`](../../src/main/java/br/com/controleagil/model).

`Entrega` e `Entregador` são da **Etapa 2** e ainda não estão no código.

```mermaid
classDiagram
    direction LR

    class Pessoa {
        <<abstract>>
        -String nome
        -String telefone
        +getNome() String
        +getTelefone() String
    }

    class Cliente {
        -Long id
        -String endereco
        -ContaFiado contaFiado
        +abrirContaFiado(BigDecimal limite) void
        +temContaFiado() boolean
        +getContaFiado() ContaFiado
    }

    class ContaFiado {
        -BigDecimal limiteCredito
        -List~MovimentacaoFiado~ movimentacoes
        +registrarCompra(BigDecimal valor, Pedido pedido) void
        +registrarPagamento(BigDecimal valor) void
        +getSaldoDevedor() BigDecimal
        +getCreditoDisponivel() BigDecimal
        +podeComprar(BigDecimal valor) boolean
        +getMovimentacoes() List~MovimentacaoFiado~
    }

    class MovimentacaoFiado {
        -LocalDateTime data
        -BigDecimal valor
        -TipoMovimentacao tipo
        -Pedido pedido
        +getValor() BigDecimal
        +getTipo() TipoMovimentacao
    }

    class TipoMovimentacao {
        <<enumeration>>
        COMPRA
        PAGAMENTO
    }

    class Pedido {
        -Long id
        -Cliente cliente
        -List~ItemPedido~ itens
        -List~Pagamento~ pagamentos
        -LocalDateTime dataCriacao
        -LocalDateTime dataHoraEntrega
        -StatusPedido status
        +adicionarItem(Produto produto, int quantidade) void
        +removerItem(ItemPedido item) void
        +calcularTotal() BigDecimal
        +registrarPagamento(Pagamento pagamento) void
        +calcularValorPago() BigDecimal
        +alterarStatus(StatusPedido novoStatus) void
        +estaProximoDaEntrega(int minutos) boolean
    }

    class StatusPedido {
        <<enumeration>>
        PENDENTE
        EM_PRODUCAO
        SAIU_PARA_ENTREGA
        ENTREGUE
        CANCELADO
        +isFinal() boolean
    }

    class ItemPedido {
        -Produto produto
        -int quantidade
        -BigDecimal precoUnitario
        +calcularSubtotal() BigDecimal
    }

    class Produto {
        -Long id
        -String nome
        -BigDecimal preco
        +getPreco() BigDecimal
        +alterarPreco(BigDecimal novoPreco) void
    }

    class Pagamento {
        <<abstract>>
        -BigDecimal valor
        -LocalDateTime data
        +getValor() BigDecimal
        +processar(Pedido pedido)* void
    }

    class PagamentoDinheiro {
        -BigDecimal valorRecebido
        +calcularTroco() BigDecimal
        +processar(Pedido pedido) void
    }

    class PagamentoPix {
        -String codigoTransacao
        +processar(Pedido pedido) void
    }

    class PagamentoFiado {
        +processar(Pedido pedido) void
    }

    Pessoa <|-- Cliente
    Cliente "1" *-- "0..1" ContaFiado : possui
    ContaFiado "1" *-- "0..*" MovimentacaoFiado : registra
    MovimentacaoFiado "0..*" --> "0..1" Pedido : originada por
    MovimentacaoFiado ..> TipoMovimentacao
    Cliente "1" <-- "0..*" Pedido : faz
    Pedido "1" *-- "1..*" ItemPedido : contém
    ItemPedido "0..*" --> "1" Produto : refere-se a
    Pedido "1" *-- "0..*" Pagamento : é pago por
    Pedido ..> StatusPedido
    Pagamento <|-- PagamentoDinheiro
    Pagamento <|-- PagamentoPix
    Pagamento <|-- PagamentoFiado
    PagamentoFiado ..> ContaFiado : usa
```

## Regras de negócio no código

| Regra | Onde |
|-------|------|
| Todo pedido nasce `PENDENTE` e precisa de data e hora de entrega | `Pedido` (construtor) |
| Itens só mudam enquanto o pedido está `PENDENTE` | `Pedido.adicionarItem` / `removerItem` |
| Status só anda para frente; `CANCELADO` vale em qualquer etapa não final | `Pedido.alterarStatus` |
| Não se paga mais do que falta pagar, nem pedido cancelado | `Pedido.registrarPagamento` |
| Pedidos atrasados também aparecem no alerta de entrega próxima | `Pedido.estaProximoDaEntrega` |
| O preço do item é o do momento da venda | `ItemPedido` (construtor) |
| Saldo devedor é calculado pelas movimentações, nunca alterado direto | `ContaFiado.getSaldoDevedor` |
| Compra fiado não pode passar do limite de crédito | `ContaFiado.registrarCompra` |
| Pagamento do fiado não pode ser maior que a dívida | `ContaFiado.registrarPagamento` |
| Fiado só para cliente com conta fiado aberta | `PagamentoFiado.processar` |
| Dinheiro: valor recebido precisa cobrir o pagamento | `PagamentoDinheiro.processar` |
| Pix: precisa do código da transação | `PagamentoPix.processar` |
