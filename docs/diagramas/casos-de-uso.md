# Casos de uso: Controle Ágil (Etapa 1)

Quem usa o sistema é a equipe da distribuidora (atendimento e caixa). O cliente final não acessa o sistema: os pedidos continuam chegando pelo WhatsApp e a equipe os registra.

```mermaid
flowchart LR
    equipe(["Equipe da distribuidora"])

    subgraph sistema["Controle Ágil"]
        uc1([Cadastrar cliente])
        uc2([Cadastrar produto])
        uc3([Registrar pedido agendado])
        uc4([Atualizar status do pedido])
        uc5([Ver agenda do dia])
        uc6([Ver alertas de entrega])
        uc7([Abrir conta fiado])
        uc8([Receber pagamento])
        uc9([Registrar pagamento do fiado])
        uc10([Ver relatório de devedores])
    end

    equipe --- uc1
    equipe --- uc2
    equipe --- uc3
    equipe --- uc4
    equipe --- uc5
    equipe --- uc6
    equipe --- uc7
    equipe --- uc8
    equipe --- uc9
    equipe --- uc10

    uc3 -. inclui .-> uc1
    uc8 -. "se for fiado, usa" .-> uc7
```

| Caso de uso | Requisitos | Classes envolvidas |
|-------------|------------|--------------------|
| Cadastrar cliente | RF01 | `Cliente`, `ClienteRepository`, `ClientesController` |
| Cadastrar produto | RF02 | `Produto`, `ProdutoRepository` |
| Registrar pedido agendado | RF03 | `Pedido`, `ItemPedido`, `PedidoService` |
| Atualizar status do pedido | RF04 | `Pedido`, `StatusPedido` |
| Ver agenda do dia | RF05 | `PedidoService.agendaDoDia()` |
| Ver alertas de entrega | RF06 | `PedidoService.entregasProximas()` |
| Abrir conta fiado | RF07 | `Cliente.abrirContaFiado()`, `ContaFiado` |
| Receber pagamento | RF12 | `Pagamento`, `PagamentoDinheiro`, `PagamentoPix`, `PagamentoFiado` |
| Registrar pagamento do fiado | RF08, RF09, RF11 | `ContaFiado`, `MovimentacaoFiado` |
| Ver relatório de devedores | RF10 | `FiadoService`, `Devedor` |
