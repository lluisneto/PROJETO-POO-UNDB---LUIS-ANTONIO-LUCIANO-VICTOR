# Requisitos do Controle Ágil

Levantamento feito com a **A L Salgados Distribuidora** a partir de três problemas da operação:

1. Pedidos chegam pelo WhatsApp e, pelo volume de mensagens, alguns não são agendados e perdem o horário.
2. O fiado (venda a prazo) é anotado em caderno: não se sabe direito quem deve, quanto e desde quando.
3. A equipe de entrega é pequena para a demanda.

Os problemas 1 e 2 formam a **Etapa 1 (MVP)**. O problema 3 fica para a **Etapa 2**.

> A numeração de RF e RN segue o **levantamento original com o cliente**. As regras que surgiram depois,
> durante a modelagem e o código, estão em **Regras complementares (RC)**, logo abaixo das RN.

## Requisitos funcionais (RF)

O que o sistema precisa fazer. "Situação" diz como está no sistema (MVP, com os dados salvos no SQLite).

### Clientes e produtos

| Código | Requisito | Etapa | Situação |
|--------|-----------|-------|----------|
| RF01 | Cadastrar, editar e listar clientes (nome, telefone, endereço) | 1 | Pronto (tela Clientes) |
| RF02 | Cadastrar, editar e listar produtos (nome e preço) | 1 | Pronto (tela Produtos) |

### Pedidos

| Código | Requisito | Etapa | Situação |
|--------|-----------|-------|----------|
| RF03 | Registrar pedido com cliente, itens, quantidades, **data e horário de entrega** | 1 | Pronto (tela Novo pedido) |
| RF04 | Calcular automaticamente o valor total do pedido | 1 | Pronto (`Pedido.calcularTotal()`) |
| RF05 | Alterar o status do pedido: Pendente, Em produção, Saiu para entrega, Entregue, Cancelado | 1 | Pronto (tela Agenda do dia) |
| RF06 | Exibir a agenda do dia, com os pedidos ordenados por horário | 1 | Pronto (tela Agenda do dia) |
| RF07 | Destacar pedidos com entrega próxima | 1 | Pronto: "Em breve" (próximos 60 min) e "Atrasado" 🔎 *(confirmar os 60 min com o cliente)* |

### Fiado

| Código | Requisito | Etapa | Situação |
|--------|-----------|-------|----------|
| RF08 | Registrar compra fiado, lançando o valor na conta do cliente | 1 | Pronto (pagamento "Fiado" no pedido) |
| RF09 | Registrar pagamento (total ou parcial) | 1 | Pronto (tela Fiado) |
| RF10 | Calcular e exibir o saldo devedor de cada cliente | 1 | Pronto (`ContaFiado.getSaldoDevedor()`) |
| RF11 | Definir um limite de crédito por cliente | 1 | Pronto (Abrir conta fiado, tela Clientes) |
| RF12 | Gerar relatório de devedores (quem deve, quanto e desde quando) | 1 | Pronto (tela Fiado e `FiadoService.relatorioDevedores()`) |
| RF13 | Exibir o histórico de compras e pagamentos de cada cliente | 1 | Pronto (tela Fiado) |

### Pagamentos e Etapa 2

| Código | Requisito | Etapa | Situação |
|--------|-----------|-------|----------|
| RF14 | Receber pagamento em dinheiro (com troco), Pix ou fiado, de uma vez ou em partes | 1 | Pronto (Novo pedido e Agenda do dia) |
| RF15 | Cadastrar entregadores fixos e avulsos | 2 | Futuro |
| RF16 | Atribuir pedidos a entregadores e agrupar por bairro e horário | 2 | Futuro |
| RF17 | Permitir retirada no local | 2 | Futuro |

## Regras de negócio (RN)

Regras do negócio do cliente que o sistema não deixa quebrar. Quando uma regra é quebrada, o sistema
lança `RegraNegocioException` com uma mensagem em português, e a tela mostra essa mensagem.

| Código | Regra | Onde está |
|--------|-------|-----------|
| RN01 | Todo pedido precisa ter data e horário de entrega | Construtor de `Pedido` |
| RN02 | Um pedido precisa ter pelo menos um item para ser salvo | `PedidoService.registrarPedido()` |
| RN03 | Uma compra fiado não pode ultrapassar o limite de crédito do cliente (**neste esqueleto, bloqueia**) 🔎 | `ContaFiado.registrarCompra()` usando `podeComprar()` |
| RN04 | Um pagamento de fiado não pode ser maior que o saldo devedor, nem menor ou igual a zero | `ContaFiado.registrarPagamento()` |
| RN05 | Pedido ENTREGUE ou CANCELADO não muda mais de status | `Pedido.alterarStatus()` usando `StatusPedido.isFinal()` |

### Regras complementares (RC)

Surgiram durante a modelagem e o código. Nenhuma regra da versão anterior deste documento foi perdida:
elas estão aqui (veja a coluna "Antes").

| Código | Regra | Onde está | Antes |
|--------|-------|-----------|-------|
| RC01 | Todo pedido nasce PENDENTE | Construtor de `Pedido` | RN01 |
| RC02 | O status só avança em ordem, um passo por vez: Pendente → Em produção → Saiu para entrega → Entregue | `Pedido.alterarStatus()` e `StatusPedido.proximo()` | RN03 |
| RC03 | CANCELADO pode vir de qualquer status que ainda não seja final | `Pedido.alterarStatus()` | RN03 |
| RC04 | Os itens só mudam enquanto o pedido está PENDENTE | `Pedido.adicionarItem()` / `removerItem()` | RN02 |
| RC05 | O item guarda o preço do momento da venda (preço da época) | `ItemPedido` | RN04 |
| RC06 | Adicionar de novo um produto que já está no pedido soma a quantidade | `Pedido.adicionarItem()`, `PedidoService.adicionarAoRascunho()` | – |
| RC07 | A soma dos pagamentos não pode passar do total; pedido cancelado não recebe pagamento | `Pedido.registrarPagamento()` | RN05 |
| RC08 | O saldo devedor é calculado pelas movimentações, nunca digitado | `ContaFiado.getSaldoDevedor()` | RN08 |
| RC09 | Só compra no fiado quem tem conta fiado aberta | `PagamentoFiado.processar()` | RN09 |
| RC10 | No dinheiro, o valor recebido precisa cobrir o pagamento (e o sistema calcula o troco) | `PagamentoDinheiro.processar()` | RN10 |
| RC11 | No Pix, o código da transação é **opcional**; se for informado, precisa ter formato válido | `PagamentoPix.processar()` | RN11 (era obrigatório) |
| RC12 | A data e hora de entrega de um pedido novo não pode estar no passado | `PedidoService.registrarPedido()` | – |
| RC13 | Quantidade do item de pelo menos 1; preço do produto maior que zero; limite de crédito zero ou mais | `ItemPedido`, `Produto`, `ContaFiado` | – |
| RC14 | Nome do cliente e do produto são obrigatórios | `Pessoa`, `Produto` | – |
| RC15 | Cada cliente tem no máximo uma conta fiado | `Cliente.abrirContaFiado()` | – |

## Requisitos não funcionais (RNF)

Como o sistema precisa ser.

| Código | Requisito |
|--------|-----------|
| RNF01 | Aplicação desktop em Java 21 com telas em JavaFX 21 |
| RNF02 | Dados salvos localmente em SQLite, num único arquivo (`controle-agil.db`), sem servidor e sem internet |
| RNF03 | Valores em dinheiro com `BigDecimal` (2 casas, arredondamento HALF_EVEN), para não errar centavos |
| RNF04 | Código separado em camadas: `model`, `repository`, `service`, `controller` e `view` |
| RNF05 | Regras de negócio cobertas por testes automatizados com JUnit |
| RNF06 | Mensagens de erro em português, dizendo o que corrigir |
| RNF07 | Interface simples, usável por quem não tem experiência com sistemas; cor nunca é a única informação |

## Perguntas pendentes para o cliente 🔎

- Ao passar do limite do fiado, o sistema deve **bloquear** a venda (como está hoje) ou só **avisar**?
- Existe prazo para pagar o fiado? Cobram juros ou multa?
- Com quanto tempo de antecedência querem ser avisados de um pedido? (hoje: 60 minutos)
- Além de dinheiro, Pix e fiado, aceitam cartão?
- Se um pedido pago no fiado for cancelado, o valor deve sair da conta automaticamente?
- Os preços mudam com frequência? Existe preço diferente por cliente?
- Quantos pedidos, em média, recebem por dia? Quem vai usar o sistema no dia a dia?
- Quantos entregadores têm hoje? *(para a Etapa 2)*
