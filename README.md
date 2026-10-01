# 🥟 Controle Ágil

> Sistema desktop de **gestão de pedidos agendados** e **controle de fiado** para a **A L Salgados Distribuidora**.

![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)
![Java](https://img.shields.io/badge/Java-21%2B-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-desktop-blue)

---

## 📌 Sumário

- [Sobre o projeto](#-sobre-o-projeto)
- [O cliente](#-o-cliente)
- [Problemas identificados](#-problemas-identificados)
- [Solução proposta](#-solução-proposta)
- [Funcionalidades](#-funcionalidades)
- [Tecnologias](#-tecnologias)
- [Modelagem orientada a objetos](#-modelagem-orientada-a-objetos)
- [Estrutura de pastas](#-estrutura-de-pastas)
- [Como executar](#-como-executar)
- [Roadmap](#-roadmap)
- [Padrão de commits](#-padrão-de-commits)
- [Equipe](#-equipe)
- [Contexto acadêmico](#-contexto-acadêmico)

---

## 📖 Sobre o projeto

O **Controle Ágil** é um projeto desenvolvido ao longo do semestre na disciplina de **Programação Orientada a Objetos**. O objetivo é resolver problemas reais da operação de um cliente real, aplicando os conceitos de POO: classes, encapsulamento, herança, polimorfismo e abstração.

---

## 🏢 O cliente

| Campo | Informação |
|-------|------------|
| **Razão social** | A L Salgados Distribuidora |
| **CNPJ** | 65.738.200/0001-91 |
| **Ramo** | Distribuição de salgados |

> O cliente autorizou a divulgação do nome e do CNPJ neste repositório.

---

## ⚠️ Problemas identificados

Problemas levantados junto ao cliente:

1. **Pedidos perdidos ou atrasados:** os pedidos chegam pelo WhatsApp e, por causa do grande volume de mensagens, às vezes não são agendados e o horário de entrega é perdido.
2. **Fiado anotado em caderno:** as vendas a prazo (fiado) são controladas manualmente em um caderno, o que dificulta saber quem deve, quanto deve e desde quando.
3. **Falta de entregadores:** a equipe de entrega é pequena para a demanda.

---

## 💡 Solução proposta

Um sistema desktop dividido em módulos, desenvolvido em etapas:

| Etapa | Módulo | Problema que resolve |
|-------|--------|----------------------|
| **1 (MVP)** | Pedidos agendados | Pedidos perdidos no WhatsApp |
| **1 (MVP)** | Conta fiado | Fiado no caderno |
| **2** | Entregas | Melhor aproveitamento dos entregadores |

> **MVP** (*Minimum Viable Product*) é a primeira versão do sistema que já resolve os problemas principais do cliente.

---

## ✅ Funcionalidades

### Etapa 1: Pedidos agendados
- [ ] Cadastro de clientes
- [ ] Cadastro de produtos (salgados, preços)
- [ ] Registro de pedido com itens, **data e horário de entrega**
- [ ] Controle de status: `PENDENTE` → `EM_PRODUCAO` → `SAIU_PARA_ENTREGA` → `ENTREGUE` / `CANCELADO`
- [ ] Agenda do dia (pedidos ordenados por horário)
- [ ] Alerta de pedidos com entrega próxima

### Etapa 1: Conta fiado
- [ ] Conta fiado por cliente
- [ ] Registro de compras fiado (débitos)
- [ ] Registro de pagamentos (total ou parcial)
- [ ] Cálculo automático do saldo devedor
- [ ] Limite de crédito por cliente
- [ ] Relatório de devedores (quem deve, quanto e há quanto tempo)
- [ ] Histórico de movimentações

### Etapa 2: Entregas (futuro)
- [ ] Cadastro de entregadores (fixos e avulsos)
- [ ] Atribuição de pedidos a entregadores
- [ ] Agrupamento de entregas por bairro e horário
- [ ] Opção de retirada no local

---

## 🛠️ Tecnologias

| Tecnologia | Para que serve no projeto |
|------------|---------------------------|
| **Java 21+** | Linguagem principal do sistema |
| **JavaFX** | Interface gráfica desktop (telas, botões, tabelas) |
| **SQLite** | Banco de dados local em um único arquivo, sem servidor |
| **Maven** | Gerencia as dependências (bibliotecas) e o build do projeto |
| **JUnit 5** | Testes automatizados |
| **Git + GitHub** | Controle de versão e trabalho em equipe |

---

## 🧩 Modelagem orientada a objetos

### Principais classes

| Classe | Responsabilidade |
|--------|------------------|
| `Pessoa` *(abstrata)* | Dados comuns: nome, telefone |
| `Cliente` | Herda de `Pessoa`; possui endereço e uma `ContaFiado` |
| `Produto` | Salgado vendido: nome e preço |
| `Pedido` | Cliente, itens, data/hora de entrega e status |
| `ItemPedido` | Produto + quantidade + subtotal |
| `StatusPedido` *(enum)* | Estados possíveis de um pedido |
| `ContaFiado` | Saldo devedor, limite de crédito e movimentações |
| `Pagamento` *(abstrata)* | Base para as formas de pagamento |
| `PagamentoDinheiro`, `PagamentoPix`, `PagamentoFiado` | Formas de pagamento concretas |

### Conceitos de POO aplicados

- **Encapsulamento:** o saldo da `ContaFiado` é privado e só muda pelos métodos `registrarCompra()` e `registrarPagamento()`, que validam as regras (ex.: limite de crédito).
- **Herança:** `Cliente` herda de `Pessoa` (e futuramente `Entregador` também).
- **Polimorfismo:** cada tipo de `Pagamento` implementa `processar()` do seu jeito; o `PagamentoFiado`, por exemplo, lança o valor na conta fiado do cliente.
- **Abstração:** `Pessoa` e `Pagamento` são classes abstratas, representando conceitos gerais.

> 📐 O diagrama de classes UML completo ficará em [`docs/diagramas/`](docs/diagramas/).

---

## 📁 Estrutura de pastas

```
PROJETO-POO-UNDB---LUIS-ANTONIO-LUCIANO-VICTOR/
├── docs/                        # Documentação do projeto
│   ├── diagramas/               # Diagramas UML (classes, casos de uso)
│   └── requisitos/              # Levantamento de requisitos com o cliente
├── src/
│   ├── main/
│   │   ├── java/br/com/controleagil/
│   │   │   ├── model/           # Classes do domínio (Cliente, Pedido, ContaFiado...)
│   │   │   ├── repository/      # Acesso ao banco de dados (salvar, buscar)
│   │   │   ├── service/         # Regras de negócio (ex.: validar limite do fiado)
│   │   │   └── controller/      # Ligação entre as telas e as regras
│   │   └── resources/
│   │       ├── view/            # Telas JavaFX (arquivos .fxml)
│   │       └── db/              # Script de criação do banco (schema.sql)
│   └── test/java/br/com/controleagil/   # Testes automatizados (JUnit)
├── .gitignore                   # Arquivos que o Git deve ignorar
└── README.md                    # Este arquivo
```

**Por que separar assim?** Cada pasta tem uma responsabilidade só. A tela (`controller`/`view`) não acessa o banco diretamente: ela pede ao `service`, que aplica as regras e usa o `repository`. Isso deixa o código mais organizado, mais fácil de testar e de dividir entre a equipe.

---

## ▶️ Como executar

> 🚧 Em construção. As instruções serão adicionadas quando a primeira versão do código estiver pronta.

Pré-requisitos previstos:
- JDK 21 ou superior
- Maven
- Git

```bash
# Clonar o repositório
git clone https://github.com/lluisneto/PROJETO-POO-UNDB---LUIS-ANTONIO-LUCIANO-VICTOR.git
cd PROJETO-POO-UNDB---LUIS-ANTONIO-LUCIANO-VICTOR
```

---

## 🗺️ Roadmap

- [x] Levantamento dos problemas com o cliente
- [x] Documentação inicial (README) e estrutura do repositório
- [ ] Diagrama de classes UML
- [ ] Implementação das classes do domínio (`model`)
- [ ] Banco de dados SQLite
- [ ] Módulo de pedidos agendados
- [ ] Módulo de conta fiado
- [ ] Interface gráfica (JavaFX)
- [ ] Testes automatizados
- [ ] Apresentação ao cliente
- [ ] Etapa 2: módulo de entregas

---

## 📝 Padrão de commits

Usamos mensagens de commit curtas e padronizadas:

| Prefixo | Uso | Exemplo |
|---------|-----|---------|
| `feat:` | Nova funcionalidade | `feat: cadastro de clientes` |
| `fix:` | Correção de erro | `fix: cálculo do saldo do fiado` |
| `docs:` | Documentação | `docs: atualiza README` |
| `test:` | Testes | `test: testes da ContaFiado` |
| `refactor:` | Melhoria de código sem mudar o comportamento | `refactor: separa service de pedidos` |
| `chore:` | Configuração e organização do projeto (sem código) | `chore: adiciona .gitignore` |

---

## 👥 Equipe

| Nome | GitHub |
|------|--------|
| Luis Antonio Arruda Neto | [@lluisneto](https://github.com/lluisneto) |
| Luciano Victor Costa de Carvalho |  |

---

## 🎓 Contexto acadêmico

| Campo | Informação |
|-------|------------|
| **Curso** | Engenharia de Software |
| **Disciplina** | Programação Orientada a Objetos |
| **Instituição** | UNDB |
| **Professor(a)** |SEBASTIÃO |
| **Período** | 4 |
