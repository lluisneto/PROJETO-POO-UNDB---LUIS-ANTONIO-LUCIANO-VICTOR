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

> Marcado = já funciona no sistema, com os dados salvos no banco SQLite (arquivo `controle-agil.db`).

### Etapa 1: Pedidos agendados
- [x] Cadastro de clientes
- [x] Cadastro de produtos (salgados, preços)
- [x] Registro de pedido com itens, **data e horário de entrega**
- [x] Controle de status: `PENDENTE` → `EM_PRODUCAO` → `SAIU_PARA_ENTREGA` → `ENTREGUE` / `CANCELADO`
- [x] Agenda do dia (pedidos ordenados por horário)
- [x] Alerta de pedidos com entrega próxima ("Em breve") e atrasados

### Etapa 1: Conta fiado
- [x] Conta fiado por cliente
- [x] Registro de compras fiado (débitos)
- [x] Registro de pagamentos (total ou parcial)
- [x] Cálculo automático do saldo devedor
- [x] Limite de crédito por cliente
- [x] Relatório de devedores (quem deve, quanto e há quanto tempo)
- [x] Histórico de movimentações

### Etapa 2: Entregas (futuro)
- [ ] Cadastro de entregadores (fixos e avulsos)
- [ ] Atribuição de pedidos a entregadores
- [ ] Agrupamento de entregas por bairro e horário
- [ ] Opção de retirada no local

---

## 🛠️ Tecnologias

| Tecnologia | Para que serve no projeto |
|------------|---------------------------|
| **Java 21 (LTS)** | Linguagem principal do sistema, com módulos (`module-info.java`) |
| **JavaFX 21** | Interface gráfica desktop (telas, botões, tabelas), com telas em FXML e visual em CSS |
| **SQLite** | Banco de dados local em um único arquivo (`controle-agil.db`), sem servidor e sem internet |
| **Maven** (com Maven Wrapper) | Gerencia as dependências (bibliotecas) e o build. O `mvnw.cmd` baixa o Maven sozinho |
| **JUnit 6** | Testes automatizados |
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

> 📐 Diagramas em [`docs/diagramas/`](docs/diagramas/): [classes](docs/diagramas/diagrama-de-classes.md) (com as classes de apoio) e [casos de uso](docs/diagramas/casos-de-uso.md). Requisitos em [`docs/requisitos/requisitos.md`](docs/requisitos/requisitos.md). O que cada tela faz: [`docs/interface/telas.md`](docs/interface/telas.md).

---

## 📁 Estrutura de pastas

```
PROJETO-POO-UNDB---LUIS-ANTONIO-LUCIANO-VICTOR/
├── docs/                          # Documentação do projeto
│   ├── diagramas/                 # Diagrama de classes (+ classes de apoio) e casos de uso
│   ├── interface/telas.md         # O que cada tela faz e quais regras aplica
│   └── requisitos/                # Requisitos (RF), regras de negócio (RN) e não funcionais (RNF)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── module-info.java   # Módulo Java: o que o sistema usa e o que ele libera
│   │   │   └── br/com/controleagil/
│   │   │       ├── App.java       # Abre a janela
│   │   │       ├── model/         # Domínio, igual ao diagrama (Cliente, Pedido, ContaFiado...)
│   │   │       ├── exception/     # RegraNegocioException
│   │   │       ├── repository/    # Interfaces de acesso aos dados
│   │   │       │   ├── memoria/   # Implementação em memória (usada nos testes)
│   │   │       │   └── sqlite/    # Implementação SQLite (usada pelo sistema)
│   │   │       ├── service/       # Casos de uso e regras que envolvem vários objetos + DadosExemplo
│   │   │       ├── controller/    # Uma classe por tela (finas: só chamam os services)
│   │   │       └── util/          # AppContexto (monta tudo), Formatador (R$, datas), Dialogos
│   │   └── resources/
│   │       ├── view/              # Telas JavaFX (arquivos .fxml)
│   │       ├── css/estilo.css     # Identidade visual (cores em variáveis)
│   │       └── db/schema.sql      # Script de criação do banco SQLite
│   └── test/java/br/com/controleagil/   # Testes automatizados (JUnit), espelhando os pacotes
├── .mvn/wrapper/ , mvnw , mvnw.cmd    # Maven Wrapper (não precisa instalar o Maven)
├── pom.xml                        # Dependências e build (Maven)
├── .gitignore                     # Arquivos que o Git deve ignorar
└── README.md                      # Este arquivo
```

**Por que separar assim?** Cada pasta tem uma responsabilidade só. A tela (`controller`/`view`) não acessa os dados diretamente: ela pede ao `service`, que aplica as regras e usa o `repository` pela **interface**. Por isso os mesmos services e as mesmas telas funcionam com o banco SQLite (no sistema) e com a memória (nos testes): quem escolhe é só o `App`, que monta o sistema.

---

## ▶️ Como executar

Os comandos abaixo são para o **Windows**, no terminal **PowerShell** (também funciona no terminal do VS Code).

### 1. Pré-requisitos

- **JDK 21** (recomendado: [Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21), instalador `.msi` para Windows x64; marque a opção que configura o `JAVA_HOME`).
- **Git**.
- **Não** precisa instalar o Maven: o projeto traz o **Maven Wrapper** (`mvnw.cmd`), que baixa o Maven sozinho na primeira vez (precisa de internet nessa primeira vez).

Para conferir o Java, abra um PowerShell novo e rode:

```powershell
java -version
```

Tem que aparecer `21` na versão.

### 2. Baixar o projeto

```powershell
git clone https://github.com/lluisneto/PROJETO-POO-UNDB---LUIS-ANTONIO-LUCIANO-VICTOR.git
cd PROJETO-POO-UNDB---LUIS-ANTONIO-LUCIANO-VICTOR
```

### 3. Abrir o sistema

```powershell
.\mvnw.cmd javafx:run
```

A janela abre na tela **Início**.

Na **primeira vez**, o sistema pergunta se você quer carregar os **dados de exemplo** (fictícios, para conhecer as telas) ou **começar vazio** (para usar com os dados de verdade da distribuidora).

Tudo fica salvo no arquivo **`controle-agil.db`**, na pasta do projeto (o arquivo fica fora do Git). Fechou e abriu de novo, os dados continuam lá.

- **Fazer cópia de segurança:** com o sistema fechado, copie o `controle-agil.db` para outro lugar (pendrive, nuvem).
- **Recomeçar do zero:** com o sistema fechado, apague o `controle-agil.db`. Na próxima vez, a pergunta da primeira vez aparece de novo.

### 4. Rodar os testes automatizados

```powershell
.\mvnw.cmd test
```

Para apagar o que foi compilado antes e testar do zero: `.\mvnw.cmd clean test`.

### 5. Abrir no VS Code

1. Instale a extensão **Extension Pack for Java** (da Microsoft).
2. No VS Code, use **File > Open Folder...** e escolha a pasta do projeto. A extensão reconhece o `pom.xml` sozinha (espere terminar o "Java: Importing projects" no rodapé).
3. Para abrir o sistema, use o terminal do VS Code (**Terminal > New Terminal**, que já é PowerShell) e rode `.\mvnw.cmd javafx:run`.
4. Os testes aparecem na aba **Testing** (ícone de tubo de ensaio) e também rodam com `.\mvnw.cmd test`.

> Se o PowerShell disser que `mvnw.cmd` não é reconhecido, confira se você está na pasta do projeto (onde fica o `pom.xml`) e se escreveu o `.\` antes.

O passo a passo para testar cada tela está em [`docs/interface/telas.md`](docs/interface/telas.md#roteiro-rápido-de-teste-manual).

---

## 🗺️ Roadmap

- [x] Levantamento dos problemas com o cliente
- [x] Documentação inicial (README) e estrutura do repositório
- [x] Diagrama de classes UML e casos de uso
- [x] Requisitos documentados (RF, RN, RNF)
- [x] Implementação das classes do domínio (`model`), seguindo o diagrama de classes
- [x] Esqueleto da interface (JavaFX): as 6 telas navegáveis, com as regras de negócio funcionando e dados de exemplo em memória
- [x] Módulo de pedidos agendados (Novo pedido, Agenda do dia, alertas "Em breve" e "Atrasado")
- [x] Módulo de conta fiado (contas, limite, histórico, pagamentos, total a receber)
- [x] Testes automatizados das regras de negócio (model, services e formatação)
- [x] Banco de dados SQLite: clientes, produtos, pedidos (itens e pagamentos) e histórico do fiado salvos no `controle-agil.db`
- [ ] Validar com o cliente as perguntas pendentes (limite do fiado bloqueia ou avisa? antecedência do alerta? cartão?)
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
