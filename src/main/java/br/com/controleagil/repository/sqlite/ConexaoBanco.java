package br.com.controleagil.repository.sqlite;

import br.com.controleagil.exception.RepositorioException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/*
 * Responsabilidade: abrir conexões com o banco SQLite, criar as tabelas (resources/db/schema.sql)
 * e rodar gravações dentro de uma TRANSAÇÃO.
 *
 * O SQLite guarda o banco inteiro num arquivo só (por padrão, controle-agil.db na pasta onde o
 * sistema roda). Não precisa instalar servidor nenhum.
 */
public class ConexaoBanco {

    /** Nome do arquivo do banco quando nenhum outro é informado. */
    public static final String ARQUIVO_PADRAO = "controle-agil.db";

    private static final String SCRIPT_TABELAS = "/db/schema.sql";

    private final String url;

    /** Banco no arquivo informado (ex.: Path.of("controle-agil.db")). */
    public ConexaoBanco(Path arquivo) {
        this("jdbc:sqlite:" + arquivo.toAbsolutePath());
    }

    /** Permite informar o endereço JDBC completo (ex.: "jdbc:sqlite:teste.db"). */
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

    /**
     * Roda o schema.sql e atualiza bancos de versões antigas. Pode ser chamado toda vez que o
     * sistema abre: não apaga nenhum dado.
     */
    public void criarTabelas() {
        String script = lerScript();
        try (Connection conexao = abrir(); Statement comando = conexao.createStatement()) {
            atualizarVersaoAntiga(conexao);
            for (String sql : script.split(";")) {
                if (!sql.isBlank()) {
                    comando.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new RepositorioException("Não foi possível criar as tabelas do banco.", e);
        }
    }

    /**
     * MIGRAÇÃO: bancos criados antes do estorno têm a tabela movimentacao_fiado aceitando só
     * 'COMPRA' e 'PAGAMENTO'. O SQLite não deixa mudar essa regra (CHECK) direto, então a tabela
     * antiga é renomeada, o schema.sql cria a nova e as linhas são copiadas de volta. Tudo numa
     * transação: se der errado, o banco fica como estava.
     */
    private void atualizarVersaoAntiga(Connection conexao) throws SQLException {
        String definicao = null;
        try (Statement comando = conexao.createStatement();
             ResultSet linha = comando.executeQuery(
                     "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'movimentacao_fiado'")) {
            if (linha.next()) {
                definicao = linha.getString(1);
            }
        }
        if (definicao == null || definicao.contains("ESTORNO")) {
            return; // banco novo (a tabela ainda não existe) ou já atualizado
        }
        conexao.setAutoCommit(false);
        try (Statement comando = conexao.createStatement()) {
            comando.execute("ALTER TABLE movimentacao_fiado RENAME TO movimentacao_fiado_antiga");
            comando.execute("CREATE TABLE movimentacao_fiado ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " cliente_id INTEGER NOT NULL REFERENCES cliente (id),"
                    + " tipo TEXT NOT NULL CHECK (tipo IN ('COMPRA', 'PAGAMENTO', 'ESTORNO')),"
                    + " valor TEXT NOT NULL,"
                    + " data TEXT NOT NULL,"
                    + " pedido_id INTEGER REFERENCES pedido (id))");
            comando.execute("INSERT INTO movimentacao_fiado (id, cliente_id, tipo, valor, data, pedido_id)"
                    + " SELECT id, cliente_id, tipo, valor, data, pedido_id FROM movimentacao_fiado_antiga");
            comando.execute("DROP TABLE movimentacao_fiado_antiga");
            conexao.commit();
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        } finally {
            conexao.setAutoCommit(true);
        }
    }

    /**
     * Um trabalho que usa a conexão e pode dar SQLException.
     * (Interface funcional: dá para passar uma lambda, como em emTransacao(conexao -> ...).)
     */
    @FunctionalInterface
    public interface Trabalho<T> {
        T executar(Connection conexao) throws SQLException;
    }

    /**
     * Roda o trabalho numa TRANSAÇÃO: ou tudo é gravado, ou nada é. Exemplo: um pedido tem a linha
     * do pedido, as linhas dos itens e as dos pagamentos; se a gravação de um item falhar, nem o
     * pedido fica salvo pela metade.
     *
     * @param mensagemDeErro texto em português, caso dê erro
     */
    public <T> T emTransacao(String mensagemDeErro, Trabalho<T> trabalho) {
        try (Connection conexao = abrir()) {
            conexao.setAutoCommit(false); // só grava de verdade no commit()
            try {
                T resultado = trabalho.executar(conexao);
                conexao.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                conexao.rollback(); // desfaz tudo o que esta transação fez
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException(mensagemDeErro, e);
        }
    }

    /** Roda uma consulta (só leitura). */
    public <T> T consultar(String mensagemDeErro, Trabalho<T> trabalho) {
        try (Connection conexao = abrir()) {
            return trabalho.executar(conexao);
        } catch (SQLException e) {
            throw new RepositorioException(mensagemDeErro, e);
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
