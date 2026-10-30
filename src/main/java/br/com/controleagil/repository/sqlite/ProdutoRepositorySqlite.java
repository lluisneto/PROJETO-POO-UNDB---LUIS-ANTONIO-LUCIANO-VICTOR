package br.com.controleagil.repository.sqlite;

import br.com.controleagil.model.Produto;
import br.com.controleagil.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/*
 * Responsabilidade: guardar os produtos no SQLite (tabela "produto").
 *
 * Conceito de POO: IMPLEMENTAÇÃO DE INTERFACE (mesmo contrato do ProdutoRepositoryMemoria).
 * Mesmo esquema do ClienteRepositorySqlite: lê tudo na abertura para um cache na memória,
 * e cada salvar() grava no banco e atualiza o cache.
 */
public class ProdutoRepositorySqlite implements ProdutoRepository {

    private static final Comparator<Produto> POR_NOME =
            Comparator.comparing(Produto::getNome, Collator.getInstance(Locale.of("pt", "BR")));

    private final ConexaoBanco banco;
    private final Map<Long, Produto> produtos = new LinkedHashMap<>();

    /** Lê todos os produtos do banco. */
    public ProdutoRepositorySqlite(ConexaoBanco banco) {
        this.banco = banco;
        carregarProdutos();
    }

    @Override
    public Produto salvar(Produto produto) {
        long id = banco.emTransacao("Não foi possível salvar o produto " + produto.getNome() + ".",
                conexao -> gravarProduto(conexao, produto));
        produto.setId(id);
        produtos.put(id, produto);
        return produto;
    }

    @Override
    public Optional<Produto> buscarPorId(long id) {
        return Optional.ofNullable(produtos.get(id));
    }

    @Override
    public List<Produto> listarTodos() {
        List<Produto> lista = new ArrayList<>(produtos.values());
        lista.sort(POR_NOME);
        return List.copyOf(lista);
    }

    private void carregarProdutos() {
        banco.consultar("Não foi possível ler os produtos do banco.", conexao -> {
            try (PreparedStatement sql = conexao.prepareStatement("SELECT id, nome, preco FROM produto ORDER BY id");
                 ResultSet linha = sql.executeQuery()) {
                while (linha.next()) {
                    Produto produto = new Produto(linha.getString("nome"), new BigDecimal(linha.getString("preco")));
                    produto.setId(linha.getLong("id"));
                    produtos.put(produto.getId(), produto);
                }
            }
            return null;
        });
    }

    /** INSERT (produto novo) ou UPDATE (produto que já tem id). Devolve o id. */
    private long gravarProduto(Connection conexao, Produto produto) throws SQLException {
        if (produto.getId() == null) {
            try (PreparedStatement sql = conexao.prepareStatement("INSERT INTO produto (nome, preco) VALUES (?, ?)")) {
                sql.setString(1, produto.getNome());
                sql.setString(2, produto.getPreco().toPlainString());
                sql.executeUpdate();
            }
            return ConexaoBanco.ultimoIdInserido(conexao);
        }
        try (PreparedStatement sql = conexao.prepareStatement("UPDATE produto SET nome = ?, preco = ? WHERE id = ?")) {
            sql.setString(1, produto.getNome());
            sql.setString(2, produto.getPreco().toPlainString());
            sql.setLong(3, produto.getId());
            sql.executeUpdate();
        }
        return produto.getId();
    }
}
