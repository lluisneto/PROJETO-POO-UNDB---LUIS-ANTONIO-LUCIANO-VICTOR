package br.com.controleagil.repository.sqlite;

import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.ItemPedido;
import br.com.controleagil.model.Pagamento;
import br.com.controleagil.model.PagamentoCartao;
import br.com.controleagil.model.PagamentoDinheiro;
import br.com.controleagil.model.PagamentoFiado;
import br.com.controleagil.model.PagamentoPix;
import br.com.controleagil.model.Pedido;
import br.com.controleagil.model.Produto;
import br.com.controleagil.model.StatusPedido;
import br.com.controleagil.model.TipoCartao;
import br.com.controleagil.repository.PedidoRepository;
import br.com.controleagil.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
 * Responsabilidade: guardar os pedidos no SQLite: a linha do pedido (tabela "pedido"), os itens com
 * o preço da época ("item_pedido") e os pagamentos ("pagamento"). O id do pedido é o número do pedido.
 *
 * Conceito de POO: IMPLEMENTAÇÃO DE INTERFACE (mesmo contrato do PedidoRepositoryMemoria).
 * Mesmo esquema dos outros repositórios SQLite: lê tudo na abertura para um cache e grava a cada salvar().
 *
 * Para remontar um pedido, ele precisa do Cliente e dos Produtos: por isso recebe os outros dois
 * repositórios, que já estão carregados quando este é criado (veja BancoSqlite).
 *
 * salvar() grava o pedido E a conta fiado do cliente numa transação só: se o pedido foi pago no
 * fiado, a compra entra na conta junto; se algo falhar, nem o pedido nem a compra ficam no banco.
 */
public class PedidoRepositorySqlite implements PedidoRepository {

    private static final Comparator<Pedido> POR_HORARIO_DE_ENTREGA =
            Comparator.comparing(Pedido::getDataHoraEntrega).thenComparing(Pedido::getId);

    private final ConexaoBanco banco;
    private final ClienteRepositorySqlite clientes;
    private final ProdutoRepository produtos;
    private final Map<Long, Pedido> pedidos = new LinkedHashMap<>();

    /** Lê todos os pedidos do banco, com itens e pagamentos. */
    public PedidoRepositorySqlite(ConexaoBanco banco, ClienteRepositorySqlite clientes, ProdutoRepository produtos) {
        this.banco = banco;
        this.clientes = clientes;
        this.produtos = produtos;
        carregarPedidos();
    }

    @Override
    public Pedido salvar(Pedido pedido) {
        String nome = pedido.getId() == null ? "o pedido novo" : "o pedido nº " + pedido.getId();
        long id = banco.emTransacao("Não foi possível salvar " + nome + ".", conexao -> {
            long idGravado = gravarPedido(conexao, pedido);
            gravarItens(conexao, idGravado, pedido);
            gravarPagamentos(conexao, idGravado, pedido);
            // Na MESMA transação: a conta fiado do cliente (o pedido pode ter lançado uma compra ou um estorno).
            clientes.gravarHistoricoNaTransacao(conexao, pedido.getCliente(), pedido, idGravado);
            return idGravado;
        });
        pedido.setId(id);
        pedidos.put(id, pedido);
        return pedido;
    }

    @Override
    public Optional<Pedido> buscarPorId(long id) {
        return Optional.ofNullable(pedidos.get(id));
    }

    @Override
    public List<Pedido> listarTodos() {
        return pedidos.values().stream().sorted(POR_HORARIO_DE_ENTREGA).toList();
    }

    @Override
    public List<Pedido> listarPorDataEntrega(LocalDate dia) {
        return pedidos.values().stream()
                .filter(pedido -> pedido.getDataHoraEntrega().toLocalDate().equals(dia))
                .sorted(POR_HORARIO_DE_ENTREGA)
                .toList();
    }

    // ------------------------------------------------------------------ leitura

    private void carregarPedidos() {
        banco.consultar("Não foi possível ler os pedidos do banco.", conexao -> {
            try (PreparedStatement sql = conexao.prepareStatement(
                    "SELECT id, cliente_id, data_criacao, data_hora_entrega, status FROM pedido ORDER BY id");
                 ResultSet linha = sql.executeQuery()) {
                while (linha.next()) {
                    long id = linha.getLong("id");
                    long idCliente = linha.getLong("cliente_id");
                    Cliente cliente = clientes.buscarPorId(idCliente)
                            .orElseThrow(() -> new SQLException("O pedido nº " + id + " é de um cliente que não existe."));
                    Pedido pedido = Pedido.restaurar(id, cliente,
                            LocalDateTime.parse(linha.getString("data_hora_entrega")),
                            LocalDateTime.parse(linha.getString("data_criacao")),
                            StatusPedido.valueOf(linha.getString("status")),
                            lerItens(conexao, id), lerPagamentos(conexao, id));
                    pedidos.put(id, pedido);
                }
            }
            return null;
        });
    }

    private List<ItemPedido> lerItens(Connection conexao, long idPedido) throws SQLException {
        List<ItemPedido> itens = new ArrayList<>();
        try (PreparedStatement sql = conexao.prepareStatement(
                "SELECT produto_id, quantidade, preco_unitario FROM item_pedido WHERE pedido_id = ? ORDER BY id")) {
            sql.setLong(1, idPedido);
            try (ResultSet linha = sql.executeQuery()) {
                while (linha.next()) {
                    long idProduto = linha.getLong("produto_id");
                    Produto produto = produtos.buscarPorId(idProduto)
                            .orElseThrow(() -> new SQLException("Item do pedido nº " + idPedido + " com produto que não existe."));
                    // O preço é o que ficou gravado (preço da época), não o preço atual do produto.
                    itens.add(ItemPedido.restaurar(produto, linha.getInt("quantidade"),
                            new BigDecimal(linha.getString("preco_unitario"))));
                }
            }
        }
        return itens;
    }

    private List<Pagamento> lerPagamentos(Connection conexao, long idPedido) throws SQLException {
        List<Pagamento> pagamentos = new ArrayList<>();
        try (PreparedStatement sql = conexao.prepareStatement(
                "SELECT forma, valor, data, valor_recebido, codigo_transacao FROM pagamento WHERE pedido_id = ? ORDER BY id")) {
            sql.setLong(1, idPedido);
            try (ResultSet linha = sql.executeQuery()) {
                while (linha.next()) {
                    BigDecimal valor = new BigDecimal(linha.getString("valor"));
                    LocalDateTime data = LocalDateTime.parse(linha.getString("data"));
                    // A coluna "forma" diz qual subclasse de Pagamento criar de volta.
                    Pagamento pagamento = switch (linha.getString("forma")) {
                        case "DINHEIRO" -> new PagamentoDinheiro(valor, new BigDecimal(linha.getString("valor_recebido")), data);
                        case "PIX" -> new PagamentoPix(valor, linha.getString("codigo_transacao"), data);
                        case "CARTAO_DEBITO" -> new PagamentoCartao(valor, TipoCartao.DEBITO, data);
                        case "CARTAO_CREDITO" -> new PagamentoCartao(valor, TipoCartao.CREDITO, data);
                        case "FIADO" -> new PagamentoFiado(valor, data);
                        default -> throw new SQLException("Forma de pagamento desconhecida: " + linha.getString("forma"));
                    };
                    pagamentos.add(pagamento);
                }
            }
        }
        return pagamentos;
    }

    // ------------------------------------------------------------------ gravação

    /** INSERT (pedido novo) ou UPDATE (pedido que já tem número). Devolve o id. */
    private long gravarPedido(Connection conexao, Pedido pedido) throws SQLException {
        if (pedido.getId() == null) {
            try (PreparedStatement sql = conexao.prepareStatement(
                    "INSERT INTO pedido (cliente_id, data_criacao, data_hora_entrega, status) VALUES (?, ?, ?, ?)")) {
                preencher(sql, pedido);
                sql.executeUpdate();
            }
            return ConexaoBanco.ultimoIdInserido(conexao);
        }
        try (PreparedStatement sql = conexao.prepareStatement(
                "UPDATE pedido SET cliente_id = ?, data_criacao = ?, data_hora_entrega = ?, status = ? WHERE id = ?")) {
            preencher(sql, pedido);
            sql.setLong(5, pedido.getId());
            sql.executeUpdate();
        }
        return pedido.getId();
    }

    private void preencher(PreparedStatement sql, Pedido pedido) throws SQLException {
        if (pedido.getCliente().getId() == null) {
            throw new SQLException("O cliente " + pedido.getCliente().getNome() + " precisa ser salvo antes do pedido.");
        }
        sql.setLong(1, pedido.getCliente().getId());
        sql.setString(2, pedido.getDataCriacao().toString());
        sql.setString(3, pedido.getDataHoraEntrega().toString());
        sql.setString(4, pedido.getStatus().name()); // name() = "EM_PRODUCAO" (o toString() é o texto da tela)
    }

    /** Regrava os itens: apaga os do pedido e grava todos de novo, na ordem. */
    private void gravarItens(Connection conexao, long idPedido, Pedido pedido) throws SQLException {
        apagarFilhos(conexao, "item_pedido", idPedido);
        try (PreparedStatement sql = conexao.prepareStatement(
                "INSERT INTO item_pedido (pedido_id, produto_id, quantidade, preco_unitario) VALUES (?, ?, ?, ?)")) {
            for (ItemPedido item : pedido.getItens()) {
                if (item.getProduto().getId() == null) {
                    throw new SQLException("O produto " + item.getProduto().getNome() + " precisa ser salvo antes do pedido.");
                }
                sql.setLong(1, idPedido);
                sql.setLong(2, item.getProduto().getId());
                sql.setInt(3, item.getQuantidade());
                sql.setString(4, item.getPrecoUnitario().toPlainString());
                sql.executeUpdate();
            }
        }
    }

    /**
     * Regrava os pagamentos. A tabela tem uma coluna "forma" (no cartão, ela já diz se foi débito ou
     * crédito) e colunas que só valem para uma das formas (valor_recebido no dinheiro,
     * codigo_transacao no Pix). Aqui é o ÚNICO lugar que precisa
     * perguntar "que tipo de pagamento é este?" (instanceof): é a tradução entre os objetos e a tabela.
     */
    private void gravarPagamentos(Connection conexao, long idPedido, Pedido pedido) throws SQLException {
        apagarFilhos(conexao, "pagamento", idPedido);
        try (PreparedStatement sql = conexao.prepareStatement(
                "INSERT INTO pagamento (pedido_id, forma, valor, data, valor_recebido, codigo_transacao) VALUES (?, ?, ?, ?, ?, ?)")) {
            for (Pagamento pagamento : pedido.getPagamentos()) {
                sql.setLong(1, idPedido);
                sql.setString(3, pagamento.getValor().toPlainString());
                sql.setString(4, pagamento.getData().toString());
                sql.setNull(5, Types.VARCHAR);
                sql.setNull(6, Types.VARCHAR);
                if (pagamento instanceof PagamentoDinheiro dinheiro) {
                    sql.setString(2, "DINHEIRO");
                    sql.setString(5, dinheiro.getValorRecebido().toPlainString());
                } else if (pagamento instanceof PagamentoPix pix) {
                    sql.setString(2, "PIX");
                    sql.setString(6, pix.getCodigoTransacao());
                } else if (pagamento instanceof PagamentoCartao cartao) {
                    sql.setString(2, "CARTAO_" + cartao.getTipo().name()); // CARTAO_DEBITO ou CARTAO_CREDITO
                } else if (pagamento instanceof PagamentoFiado) {
                    sql.setString(2, "FIADO");
                } else {
                    throw new SQLException("Forma de pagamento sem coluna no banco: " + pagamento.getDescricao());
                }
                sql.executeUpdate();
            }
        }
    }

    private static void apagarFilhos(Connection conexao, String tabela, long idPedido) throws SQLException {
        // O nome da tabela vem só deste arquivo (nunca do usuário), então pode ir no texto do SQL.
        try (PreparedStatement sql = conexao.prepareStatement("DELETE FROM " + tabela + " WHERE pedido_id = ?")) {
            sql.setLong(1, idPedido);
            sql.executeUpdate();
        }
    }
}
