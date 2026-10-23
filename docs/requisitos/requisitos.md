# Requisitos do Controle Ágil

Levantamento feito com a **A L Salgados Distribuidora** a partir de três problemas da operação:

1. Pedidos chegam pelo WhatsApp e, pelo volume de mensagens, alguns não são agendados e perdem o horário.
2. O fiado (venda a prazo) é anotado em caderno: não se sabe direito quem deve, quanto e desde quando.
3. A equipe de entrega é pequena para a demanda.

Os problemas 1 e 2 formam a **Etapa 1 (MVP)**. O problema 3 fica para a **Etapa 2**.

## Requisitos funcionais (RF)

O que o sistema precisa fazer.

| Código | Requisito | Etapa | Situação |
|--------|-----------|-------|----------|
| RF01 | Cadastrar clientes com nome, telefone e endereço | 1 | Tela e banco prontos |
| RF02 | Cadastrar produtos (salgados) com nome e preço | 1 | Banco pronto, falta a tela |
| RF03 | Registrar pedido com cliente, itens, data e hora de entrega | 1 | Regra pronta (`Pedido`) |
| RF04 | Acompanhar o status do pedido: PENDENTE, EM_PRODUCAO, SAIU_PARA_ENTREGA, ENTREGUE ou CANCELADO | 1 | Regra pronta (`Pedido`) |
| RF05 | Mostrar a agenda do dia, com os pedidos ordenados por horário | 1 | Regra pronta (`PedidoService`) |
| RF06 | Alertar os pedidos com entrega próxima ou atrasada | 1 | Regra pronta (`PedidoService`) |
| RF07 | Abrir conta fiado para um cliente, com limite de crédito | 1 | Regra pronta (`Cliente`, `ContaFiado`) |
| RF08 | Registrar compras e pagamentos no fiado | 1 | Regra pronta (`ContaFiado`) |
| RF09 | Calcular o saldo devedor automaticamente | 1 | Regra pronta (`ContaFiado`) |
| RF10 | Listar os devedores: quem deve, quanto e desde quando | 1 | Regra pronta (`FiadoService`) |
| RF11 | Mostrar o histórico de movimentações do fiado | 1 | Regra pronta (`ContaFiado`) |
| RF12 | Receber pagamento em dinheiro (com troco), Pix ou fiado | 1 | Regra pronta (`Pagamento` e filhas) |
| RF13 | Cadastrar entregadores fixos e avulsos | 2 | Futuro |
| RF14 | Atribuir pedidos a entregadores e agrupar por bairro e horário | 2 | Futuro |
| RF15 | Permitir retirada no local | 2 | Futuro |

## Regras de negócio (RN)

Condições que o sistema não deixa quebrar. Cada uma já está no código.

| Código | Regra | Onde está |
|--------|-------|-----------|
| RN01 | Todo pedido tem data e hora de entrega e nasce PENDENTE | `Pedido` |
| RN02 | Os itens só mudam enquanto o pedido está PENDENTE | `Pedido.adicionarItem()` / `removerItem()` |
| RN03 | O status só anda para frente; CANCELADO vale até o pedido ser entregue | `Pedido.alterarStatus()` |
| RN04 | O item guarda o preço do momento da venda | `ItemPedido` |
| RN05 | Não se paga mais do que falta pagar, nem um pedido cancelado | `Pedido.registrarPagamento()` |
| RN06 | Uma compra no fiado não pode passar do limite de crédito | `ContaFiado.registrarCompra()` |
| RN07 | Um pagamento no fiado não pode ser maior que a dívida | `ContaFiado.registrarPagamento()` |
| RN08 | O saldo devedor é calculado pelas movimentações, nunca digitado | `ContaFiado.getSaldoDevedor()` |
| RN09 | Só compra no fiado quem tem conta fiado aberta | `PagamentoFiado.processar()` |
| RN10 | No dinheiro, o valor recebido precisa cobrir o pagamento | `PagamentoDinheiro.processar()` |
| RN11 | No Pix, é obrigatório o código da transação | `PagamentoPix.processar()` |

## Requisitos não funcionais (RNF)

Como o sistema precisa ser.

| Código | Requisito |
|--------|-----------|
| RNF01 | Aplicação desktop em Java 21 com telas em JavaFX |
| RNF02 | Dados salvos localmente em SQLite, num único arquivo, sem servidor |
| RNF03 | Valores em dinheiro com `BigDecimal`, para não errar centavos |
| RNF04 | Código separado em camadas: `model`, `repository`, `service`, `controller` e `view` |
| RNF05 | Regras de negócio cobertas por testes automatizados com JUnit 5 |
| RNF06 | Mensagens de erro em português, dizendo o que corrigir |
