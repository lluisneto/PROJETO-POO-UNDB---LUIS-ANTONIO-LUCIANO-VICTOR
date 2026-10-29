package br.com.controleagil.repository.sqlite;

import br.com.controleagil.model.Cliente;
import br.com.controleagil.repository.ClienteRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
 * Responsabilidade: salvar e buscar clientes na tabela "cliente" do SQLite.
 * Por enquanto guarda o limite da conta fiado; as movimentações do fiado
 * (tabela movimentacao_fiado) entram na próxima etapa.
 *
 * Conceito de POO: IMPLEMENTAÇÃO DE INTERFACE. Cumpre o mesmo contrato que o
 * ClienteRepositoryMemoria; por isso, no futuro, basta trocar qual dos dois o AppContexto cria.
 * Ainda NÃO é usado pelo sistema (o esqueleto roda em memória); é testado em RepositorioSqliteTest.
 */
public class ClienteRepositorySqlite implements ClienteRepository {

    private final ConexaoBanco banco;

    public ClienteRepositorySqlite(ConexaoBanco banco) {
        this.banco = banco;
    }

    @Override
    public Cliente salvar(Cliente cliente) {
        try (Connection conexao = banco.abrir()) {
            if (cliente.getId() == null) {
                try (PreparedStatement sql = conexao.prepareStatement(
                        "INSERT INTO cliente (nome, telefone, endereco, limite_fiado) VALUES (?, ?, ?, ?)")) {
                    preencher(sql, cliente);
                    sql.executeUpdate();
                }
                cliente.setId(ConexaoBanco.ultimoIdInserido(conexao));
            } else {
                try (PreparedStatement sql = conexao.prepareStatement(
                        "UPDATE cliente SET nome = ?, telefone = ?, endereco = ?, limite_fiado = ? WHERE id = ?")) {
                    preencher(sql, cliente);
                    sql.setLong(5, cliente.getId());
                    sql.executeUpdate();
                }
            }
            return cliente;
        } catch (SQLException e) {
            throw new RepositorioException("Não foi possível salvar o cliente " + cliente.getNome() + ".", e);
        }
    }

    @Override
    public Optional<Cliente> buscarPorId(long id) {
        try (Connection conexao = banco.abrir();
             PreparedStatement sql = conexao.prepareStatement(
                     "SELECT id, nome, telefone, endereco, limite_fiado FROM cliente WHERE id = ?")) {
            sql.setLong(1, id);
            try (ResultSet linha = sql.executeQuery()) {
                return linha.next() ? Optional.of(montar(linha)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositorioException("Não foi possível buscar o cliente " + id + ".", e);
        }
    }

    @Override
    public List<Cliente> listarTodos() {
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conexao = banco.abrir();
             Statement sql = conexao.createStatement();
             ResultSet linha = sql.executeQuery(
                     "SELECT id, nome, telefone, endereco, limite_fiado FROM cliente ORDER BY nome COLLATE NOCASE")) {
            while (linha.next()) {
                clientes.add(montar(linha));
            }
            return clientes;
        } catch (SQLException e) {
            throw new RepositorioException("Não foi possível listar os clientes.", e);
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

    private Cliente montar(ResultSet linha) throws SQLException {
        Cliente cliente = new Cliente(linha.getString("nome"), linha.getString("telefone"), linha.getString("endereco"));
        String limite = linha.getString("limite_fiado");
        if (limite != null) {
            cliente.abrirContaFiado(new BigDecimal(limite));
        }
        cliente.setId(linha.getLong("id"));
        return cliente;
    }
}
