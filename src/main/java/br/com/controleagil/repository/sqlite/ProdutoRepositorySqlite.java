package br.com.controleagil.repository.sqlite;

import br.com.controleagil.model.Produto;
import br.com.controleagil.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*
 * Responsabilidade: salvar e buscar produtos na tabela "produto" do SQLite.
 *
 * Conceito de POO: IMPLEMENTAÇÃO DE INTERFACE (mesmo contrato do ProdutoRepositoryMemoria).
 * Ainda NÃO é usado pelo sistema (o esqueleto roda em memória); é testado em RepositorioSqliteTest.
 */
public class ProdutoRepositorySqlite implements ProdutoRepository {

    private final ConexaoBanco banco;

    public ProdutoRepositorySqlite(ConexaoBanco banco) {
        this.banco = banco;
    }

    @Override
    public Produto salvar(Produto produto) {
        try (Connection conexao = banco.abrir()) {
            if (produto.getId() == null) {
                try (PreparedStatement sql = conexao.prepareStatement(
                        "INSERT INTO produto (nome, preco) VALUES (?, ?)")) {
                    sql.setString(1, produto.getNome());
                    sql.setString(2, produto.getPreco().toPlainString());
                    sql.executeUpdate();
                }
                produto.setId(ConexaoBanco.ultimoIdInserido(conexao));
            } else {
                try (PreparedStatement sql = conexao.prepareStatement(
                        "UPDATE produto SET nome = ?, preco = ? WHERE id = ?")) {
                    sql.setString(1, produto.getNome());
                    sql.setString(2, produto.getPreco().toPlainString());
                    sql.setLong(3, produto.getId());
                    sql.executeUpdate();
                }
            }
            return produto;
        } catch (SQLException e) {
            throw new RepositorioException("Não foi possível salvar o produto " + produto.getNome() + ".", e);
        }
    }

    @Override
    public Optional<Produto> buscarPorId(long id) {
        try (Connection conexao = banco.abrir();
             PreparedStatement sql = conexao.prepareStatement("SELECT id, nome, preco FROM produto WHERE id = ?")) {
            sql.setLong(1, id);
            try (ResultSet linha = sql.executeQuery()) {
                return linha.next() ? Optional.of(montar(linha)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositorioException("Não foi possível buscar o produto " + id + ".", e);
        }
    }

    @Override
    public List<Produto> listarTodos() {
        List<Produto> produtos = new ArrayList<>();
        try (Connection conexao = banco.abrir();
             Statement sql = conexao.createStatement();
             ResultSet linha = sql.executeQuery("SELECT id, nome, preco FROM produto ORDER BY nome COLLATE NOCASE")) {
            while (linha.next()) {
                produtos.add(montar(linha));
            }
            return produtos;
        } catch (SQLException e) {
            throw new RepositorioException("Não foi possível listar os produtos.", e);
        }
    }

    private Produto montar(ResultSet linha) throws SQLException {
        Produto produto = new Produto(linha.getString("nome"), new BigDecimal(linha.getString("preco")));
        produto.setId(linha.getLong("id"));
        return produto;
    }
}
