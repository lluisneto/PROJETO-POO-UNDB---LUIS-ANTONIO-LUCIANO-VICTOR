package br.com.controleagil.repository.sqlite;

import br.com.controleagil.model.Cliente;
import br.com.controleagil.model.MovimentacaoFiado;
import br.com.controleagil.model.Pedido;
import br.com.controleagil.model.TipoMovimentacao;
import br.com.controleagil.repository.ClienteRepository;
import br.com.controleagil.repository.PedidoRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.Collator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/*
 * Responsabilidade: guardar os clientes no SQLite (tabela "cliente") junto com a conta fiado:
 * o limite (coluna limite_fiado) e o histórico (tabela "movimentacao_fiado").
 *
 * Conceito de POO: IMPLEMENTAÇÃO DE INTERFACE. Cumpre o mesmo contrato que o
 * ClienteRepositoryMemoria, então os services funcionam igual com um ou com outro.
 *
 * Como funciona: quando o sistema abre, todos os clientes são lidos do banco para a memória
 * (o "cache"). Cada salvar() grava no banco e atualiza o cache. Assim todas as telas usam
 * os MESMOS objetos (o mesmo cliente que está no pedido é o que está na tela do fiado),
 * e o banco guarda tudo para a próxima vez que o sistema abrir.
 */
public class ClienteRepositorySqlite implements ClienteRepository {

    private static final Comparator<Cliente> POR_NOME =
            Comparator.comparing(Cliente::getNome, Collator.getInstance(Locale.of("pt", "BR")));

    private final ConexaoBanco banco;
    private final Map<Long, Cliente> clientes = new LinkedHashMap<>();

    /** Lê todos os clientes do banco (sem o histórico do fiado, que depende dos pedidos). */
    public ClienteRepositorySqlite(ConexaoBanco banco) {
        this.banco = banco;
        carregarClientes();
    }

    @Override
    public Cliente salvar(Cliente cliente) {
        long id = banco.emTransacao("Não foi possível salvar o cliente " + cliente.getNome() + ".", conexao -> {
            long idGravado = gravarCliente(conexao, cliente);
            gravarHistoricoDoFiado(conexao, idGravado, cliente);
            return idGravado;
        });
        // Só depois que o banco confirmou (commit) o cliente ganha o id e entra no cache.
        cliente.setId(id);
        clientes.put(id, cliente);
        return cliente;
    }

    @Override
    public Optional<Cliente> buscarPorId(long id) {
        return Optional.ofNullable(clientes.get(id));
    }

    @Override
    public List<Cliente> listarTodos() {
        List<Cliente> lista = new ArrayList<>(clientes.values());
        lista.sort(POR_NOME);
        return List.copyOf(lista);
    }

    /**
     * Lê o histórico do fiado de todos os clientes. Fica separado do construtor porque cada compra
     * aponta para o PEDIDO que a gerou, e os pedidos só podem ser lidos depois dos clientes.
     * Chamado uma vez pelo BancoSqlite, na abertura do sistema.
     */
    void carregarHistoricoDoFiado(PedidoRepository pedidos) {
        banco.consultar("Não foi possível ler o histórico do fiado.", conexao -> {
            try (PreparedStatement sql = conexao.prepareStatement(
                    "SELECT cliente_id, tipo, valor, data, pedido_id FROM movimentacao_fiado ORDER BY id");
                 ResultSet linha = sql.executeQuery()) {
                while (linha.next()) {
                    Cliente cliente = clientes.get(linha.getLong("cliente_id"));
                    if (cliente == null || !cliente.temContaFiado()) {
                        throw new SQLException("Movimentação do fiado de um cliente sem conta (id "
                                + linha.getLong("cliente_id") + ").");
                    }
                    long idPedido = linha.getLong("pedido_id");
                    Pedido pedido = linha.wasNull() ? null : pedidos.buscarPorId(idPedido).orElse(null);
                    cliente.getContaFiado().restaurarMovimentacao(
                            TipoMovimentacao.valueOf(linha.getString("tipo")),
                            new BigDecimal(linha.getString("valor")),
                            LocalDateTime.parse(linha.getString("data")),
                            pedido);
                }
            }
            return null;
        });
    }

    // ------------------------------------------------------------------ apoio

    private void carregarClientes() {
        banco.consultar("Não foi possível ler os clientes do banco.", conexao -> {
            try (PreparedStatement sql = conexao.prepareStatement(
                    "SELECT id, nome, telefone, endereco, limite_fiado FROM cliente ORDER BY id");
                 ResultSet linha = sql.executeQuery()) {
                while (linha.next()) {
                    Cliente cliente = new Cliente(linha.getString("nome"), linha.getString("telefone"),
                            linha.getString("endereco"));
                    String limite = linha.getString("limite_fiado");
                    if (limite != null) {
                        cliente.abrirContaFiado(new BigDecimal(limite));
                    }
                    cliente.setId(linha.getLong("id"));
                    clientes.put(cliente.getId(), cliente);
                }
            }
            return null;
        });
    }

    /** INSERT (cliente novo) ou UPDATE (cliente que já tem id). Devolve o id. */
    private long gravarCliente(Connection conexao, Cliente cliente) throws SQLException {
        if (cliente.getId() == null) {
            try (PreparedStatement sql = conexao.prepareStatement(
                    "INSERT INTO cliente (nome, telefone, endereco, limite_fiado) VALUES (?, ?, ?, ?)")) {
                preencher(sql, cliente);
                sql.executeUpdate();
            }
            return ConexaoBanco.ultimoIdInserido(conexao);
        }
        try (PreparedStatement sql = conexao.prepareStatement(
                "UPDATE cliente SET nome = ?, telefone = ?, endereco = ?, limite_fiado = ? WHERE id = ?")) {
            preencher(sql, cliente);
            sql.setLong(5, cliente.getId());
            sql.executeUpdate();
        }
        return cliente.getId();
    }

    /**
     * Regrava o histórico do fiado do cliente: apaga as linhas dele e grava todas de novo, na ordem.
     * É o jeito mais simples de o banco ficar sempre igual ao que está na memória (os históricos
     * são pequenos, então isso é rápido).
     */
    private void gravarHistoricoDoFiado(Connection conexao, long idCliente, Cliente cliente) throws SQLException {
        try (PreparedStatement sql = conexao.prepareStatement("DELETE FROM movimentacao_fiado WHERE cliente_id = ?")) {
            sql.setLong(1, idCliente);
            sql.executeUpdate();
        }
        if (!cliente.temContaFiado()) {
            return;
        }
        try (PreparedStatement sql = conexao.prepareStatement(
                "INSERT INTO movimentacao_fiado (cliente_id, tipo, valor, data, pedido_id) VALUES (?, ?, ?, ?, ?)")) {
            for (MovimentacaoFiado movimentacao : cliente.getContaFiado().getMovimentacoes()) {
                sql.setLong(1, idCliente);
                sql.setString(2, movimentacao.getTipo().name());
                sql.setString(3, movimentacao.getValor().toPlainString());
                sql.setString(4, movimentacao.getData().toString());
                Pedido pedido = movimentacao.getPedido();
                if (pedido == null || pedido.getId() == null) {
                    sql.setNull(5, Types.INTEGER);
                } else {
                    sql.setLong(5, pedido.getId());
                }
                sql.executeUpdate();
            }
        }
    }

    private void preencher(PreparedStatement sql, Cliente cliente) throws SQLException {
        sql.setString(1, cliente.getNome());
        sql.setString(2, cliente.getTelefone());
        sql.setString(3, cliente.getEndereco());
        if (cliente.temContaFiado()) {
            sql.setString(4, cliente.getContaFiado().getLimiteCredito().toPlainString());
        } else {
            sql.setNull(4, Types.VARCHAR);
        }
    }
}
