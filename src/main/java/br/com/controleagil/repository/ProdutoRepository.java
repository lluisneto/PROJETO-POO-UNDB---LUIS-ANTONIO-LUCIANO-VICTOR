package br.com.controleagil.repository;

import br.com.controleagil.model.Produto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Salva e busca produtos na tabela produto. */
public class ProdutoRepository {

    private final ConexaoBanco banco;

    public ProdutoRepository(ConexaoBanco banco) {
        this.banco = banco;
    }

    /** Produto sem id é inserido (e ganha um id); produto com id é atualizado. */
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

    /** Todos os produtos, em ordem alfabética. */
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
