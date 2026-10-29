package br.com.controleagil.repository.sqlite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/*
 * Responsabilidade: abrir conexões com o banco SQLite e criar as tabelas (resources/db/schema.sql).
 * Por padrão o banco é o arquivo controle-agil.db, na pasta onde o sistema roda.
 *
 * Ainda não é usado pelo esqueleto da interface (que roda em memória); só pelos testes do SQLite.
 */
public class ConexaoBanco {

    private static final String URL_PADRAO = "jdbc:sqlite:controle-agil.db";
    private static final String SCRIPT_TABELAS = "/db/schema.sql";

    private final String url;

    public ConexaoBanco() {
        this(URL_PADRAO);
    }

    /** Permite apontar para outro arquivo, por exemplo um banco temporário nos testes. */
    public ConexaoBanco(String url) {
        this.url = url;
    }

    /** Quem chama deve fechar a conexão (use try-with-resources). */
    public Connection abrir() throws SQLException {
        Connection conexao = DriverManager.getConnection(url);
        try (Statement comando = conexao.createStatement()) {
            // O SQLite só respeita as chaves estrangeiras com esta opção ligada.
            comando.execute("PRAGMA foreign_keys = ON");
        }
        return conexao;
    }

    /** Roda o schema.sql. Pode ser chamado toda vez que o sistema abre: não apaga nada. */
    public void criarTabelas() {
        String script = lerScript();
        try (Connection conexao = abrir(); Statement comando = conexao.createStatement()) {
            for (String sql : script.split(";")) {
                if (!sql.isBlank()) {
                    comando.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Não foi possível criar as tabelas do banco.", e);
        }
    }

    /** Id gerado pelo último INSERT feito nesta mesma conexão. */
    static long ultimoIdInserido(Connection conexao) throws SQLException {
        try (Statement comando = conexao.createStatement();
             ResultSet linha = comando.executeQuery("SELECT last_insert_rowid()")) {
            linha.next();
            return linha.getLong(1);
        }
    }

    /** Lê o script e tira as linhas que são só comentário. */
    private String lerScript() {
        try (InputStream entrada = ConexaoBanco.class.getResourceAsStream(SCRIPT_TABELAS)) {
            if (entrada == null) {
                throw new RepositorioException("Arquivo " + SCRIPT_TABELAS + " não encontrado.", null);
            }
            StringBuilder semComentarios = new StringBuilder();
            for (String linha : new String(entrada.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (!linha.strip().startsWith("--")) {
                    semComentarios.append(linha).append('\n');
                }
            }
            return semComentarios.toString();
        } catch (IOException e) {
            throw new RepositorioException("Não foi possível ler " + SCRIPT_TABELAS + ".", e);
        }
    }
}
