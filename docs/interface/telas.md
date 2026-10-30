# Telas do Controle Ágil (esqueleto da interface)

Todas as telas funcionam de verdade, com as regras de negócio aplicadas, e os dados ficam salvos no
banco SQLite (arquivo `controle-agil.db`): fechou e abriu de novo, está tudo lá.

Na primeira vez (quando ainda não existe o `controle-agil.db`), o sistema pergunta se quer carregar
os **dados fictícios** de exemplo (classe `DadosExemplo`) ou começar vazio. Os exemplos têm horários
calculados a partir da hora em que o sistema abriu pela primeira vez. Assim a agenda sempre tem um pedido atrasado, um "em breve" e outros mais tarde.
Em horário de expediente tudo fica no dia de hoje; de madrugada, os pedidos já entregues e o atrasado
ficam na noite anterior, e à noite os pedidos "mais tarde" vão para a manhã seguinte.

As tabelas não reordenam ao clicar no cabeçalho: a ordem de cada lista é a do negócio (agenda por
horário, fiado de quem deve mais para quem deve menos, cadastros por nome).

Os códigos de regra (RN, RC) e de requisito (RF) são os de [`docs/requisitos/requisitos.md`](../requisitos/requisitos.md).

## Janela principal

| Parte | O que faz |
|-------|-----------|
| Menu lateral (fundo escuro) | Botões **Início**, **Agenda do dia**, **Novo pedido**, **Clientes**, **Produtos** e **Fiado**. A tela aberta fica destacada em laranja. O menu troca só o centro da janela. |
| Cabeçalho | Título da tela e a data de hoje por extenso (ex.: "quinta-feira, 29 de outubro de 2026"). |
| Barra de status | Mensagens de sucesso (ex.: "Pedido nº 12 salvo para hoje às 15:30.") que somem depois de alguns segundos. |
| Avisos | Regra de negócio quebrada vira um aviso com a mensagem da regra. Erro inesperado vira um aviso genérico e o detalhe técnico vai para o console. |

Tamanho inicial 1280×800, mínimo 1100×700. A Agenda e o Início se atualizam sozinhos a cada 30 segundos,
para um pedido "Em breve" virar "Atrasado" quando a hora passar.

## Início (painel)

| Elemento | O que mostra |
|----------|--------------|
| Pedidos de hoje | Quantos pedidos têm entrega hoje (sem contar os cancelados). |
| Próximas 2 horas *(cartão em destaque)* | Pedidos em aberto com entrega nas próximas 2 horas, e quantos estão atrasados. |
| A receber no fiado | Soma dos saldos devedores de todas as contas. |
| Clientes devendo | Quantos clientes têm saldo devedor, de quantos com conta fiado. |
| Próximas entregas | Até 5 pedidos de hoje ainda não finalizados, por horário: hora, cliente, número, total, status e "Em breve"/"Atrasado". Clicar abre a Agenda do dia. |

Regras usadas: RF06, RF07.

## Agenda do dia

| Elemento | O que faz |
|----------|-----------|
| Filtros | Dia (padrão: hoje, botão **Hoje** volta para hoje) e status (Todos ou um status). |
| Tabela | Hora · Nº · Cliente · Itens (ex.: "3× Coxinha, 2× Kibe") · Total · Pago · Status, por horário. |
| Etiquetas | Status com cor **e texto** (Pendente, Em produção, Saiu para entrega, Entregue, Cancelado riscado). |
| Em breve | Pedido não finalizado com entrega nos próximos 60 minutos: linha destacada e etiqueta "Em breve". |
| Atrasado | Pedido não finalizado com a hora já passada: etiqueta "Atrasado". |
| Avançar status | Mostra para onde vai (ex.: "Avançar para Em produção"). Desabilitado para pedido entregue ou cancelado. |
| Registrar pagamento | Diálogo com total, já pago, falta pagar, forma (dinheiro com troco, Pix, fiado com crédito disponível) e valor (total ou parcial). Se a regra recusar, a mensagem aparece dentro do diálogo. |
| Ver detalhes | Diálogo com cliente, entrega, itens com o preço da época, pagamentos, total, pago e falta pagar. Também abre com dois cliques na linha. |
| Cancelar pedido | Pede confirmação. Avisa se o pedido já tem pagamentos registrados. |
| Estado vazio | "Nenhum pedido para este dia" com o botão **Novo pedido**. |

Regras usadas: RN05, RC02, RC03, RC07, RN03 e RN04 (pagamento fiado), RF05, RF06, RF07, RF14.

## Novo pedido

| Elemento | O que faz |
|----------|-----------|
| Cliente | Lista com nome e telefone. |
| Entrega | Dia (padrão: hoje) e hora: lista de 15 em 15 minutos, das 06:00 às 22:00, ou digitada no formato HH:mm (validada). O sistema sugere o próximo horário redondo. |
| Itens | Produto (com preço) + quantidade + **Adicionar**. Adicionar um produto que já está na lista **soma a quantidade**. Cada linha tem **Remover**. |
| Total | Em destaque, atualizado na hora. |
| Pagamento | **Pagar depois** (padrão), **Dinheiro** (valor recebido e troco calculado na hora), **Pix** (código opcional) ou **Fiado** (mostra o crédito disponível e avisa se o total passa do limite; desabilitado, com explicação, se o cliente não tiver conta fiado). |
| Salvar / Limpar | Salvar registra o pedido e mostra o número na barra de status. Erros aparecem logo acima do botão Salvar. |

Regras usadas: RN01, RN02, RN03 (fiado acima do limite **bloqueia** o pedido inteiro), RC06, RC10, RC11, RC12, RF03, RF04, RF08, RF14.

## Clientes

| Elemento | O que faz |
|----------|-----------|
| Busca | Filtra enquanto digita, por nome (sem ligar para acentos e maiúsculas) ou por pedaço do telefone. |
| Tabela | Nome · Telefone · Endereço · Conta fiado (Sim/Não) · Saldo devedor. |
| Formulário | Clicar num cliente da tabela leva ele para o formulário (edição); o botão **Novo cliente** limpa o formulário (cadastro). Usar a busca no meio da edição não apaga o que já foi digitado. Nome obrigatório. |
| Abrir conta fiado | Pede o limite de crédito. Desabilitado se o cliente já tiver conta. |

Regras usadas: RC14, RC13 (limite ≥ 0), RC15, RF01, RF11.

## Produtos

| Elemento | O que faz |
|----------|-----------|
| Tabela | Nome · Preço. |
| Formulário | Cadastra ou edita (nome e preço). O preço muda por `Produto.alterarPreco()`. |
| Dica | Explica que os pedidos antigos **mantêm o preço da época**: cada item guardou o preço do dia da venda. |

Regras usadas: RC05, RC13 (preço > 0), RC14, RF02.

## Fiado

| Elemento | O que faz |
|----------|-----------|
| Contas | Cliente · Limite · Saldo devedor · Crédito disponível · Última compra, de quem deve mais para quem deve menos. Opção **Mostrar só quem deve**. |
| Conta selecionada | Saldo devedor em destaque, "devendo desde", barra de uso do limite com texto (ex.: "R$ 120,00 de R$ 200,00 usados"; fica vermelha a partir de 90%). |
| Histórico | Data · Tipo (Compra/Pagamento) · Valor · Pedido nº, do mais novo para o mais antigo. |
| Registrar pagamento | Valor + botão. Pagamento válido reduz o saldo e entra no histórico. |
| Rodapé | Total a receber somando todas as contas e quantos clientes estão devendo. |

Regras usadas: RN04, RC08, RF08 a RF13.

## Roteiro rápido de teste manual

1. Abra o sistema (`.\mvnw.cmd javafx:run`). Na primeira vez, escolha **Carregar exemplos**. O **Início** mostra os cartões e as próximas entregas.
2. Clique em cada item do menu: as 6 telas abrem e o item ativo fica laranja.
3. **Agenda do dia**: confira um pedido "Atrasado" e um "Em breve" (linha destacada).
4. Selecione um pedido pendente e clique em **Avançar para Em produção** até **Entregue**. Depois disso, os botões de avançar e cancelar ficam desabilitados.
5. **Novo pedido**: escolha **Mercadinho Bom Preço** (crédito de R$ 45,00), adicione 10 coxinhas, escolha **Fiado** e salve: o pedido é **bloqueado** com mensagem. Troque para **Pagar depois**, adicione mais um item e salve: o pedido aparece na Agenda.
6. **Fiado**: selecione a **Lanchonete Sabor da Praça** (deve R$ 30,00), digite 50 e registre: recusado (maior que o saldo). Digite 10 e registre: o saldo cai para R$ 20,00 e o pagamento aparece no histórico.
7. **Produtos**: mude o preço da coxinha para 6,00. Na Agenda, o pedido nº 1 continua com o total antigo (R$ 100,00).
8. **Clientes**: busque por "pao" (sem acento) e por "0104"; abra a conta fiado de um cliente sem conta.
9. Feche o sistema e abra de novo: tudo o que você fez nos passos anteriores continua lá (está no `controle-agil.db`).
