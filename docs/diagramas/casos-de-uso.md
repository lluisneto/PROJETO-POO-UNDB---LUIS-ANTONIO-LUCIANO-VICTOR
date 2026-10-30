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

| Caso de uso | Requisitos | Tela | Classes envolvidas |
|-------------|------------|------|--------------------|
| Cadastrar cliente | RF01 | Clientes | `Cliente`, `ClienteService`, `ClientesController` |
| Cadastrar produto | RF02 | Produtos | `Produto`, `ProdutoService`, `ProdutosController` |
| Registrar pedido agendado | RF03, RF04 | Novo pedido | `Pedido`, `ItemPedido`, `PedidoService`, `NovoPedidoController` |
| Atualizar status do pedido | RF05 | Agenda do dia | `Pedido`, `StatusPedido`, `PedidoService.avancarStatus()` |
| Ver agenda do dia | RF06 | Agenda do dia | `PedidoService.pedidosDoDia()`, `AgendaController` |
| Ver alertas de entrega | RF07 | Início e Agenda do dia | `PedidoService.estaEmBreve()` / `estaAtrasado()` |
| Abrir conta fiado | RF11 | Clientes | `Cliente.abrirContaFiado()`, `ContaFiado` |
| Receber pagamento | RF14, RF08 | Novo pedido e Agenda do dia | `Pagamento`, `PagamentoDinheiro`, `PagamentoPix`, `PagamentoFiado` |
| Registrar pagamento do fiado | RF09, RF10, RF13 | Fiado | `FiadoService`, `ContaFiado`, `MovimentacaoFiado` |
| Ver relatório de devedores | RF12 | Fiado e Início | `FiadoService`, `Devedor` |
