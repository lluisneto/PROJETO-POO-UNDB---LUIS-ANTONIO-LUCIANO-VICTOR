package br.com.controleagil.repository.sqlite;

import br.com.controleagil.repository.Repositorios;

/*
 * Responsabilidade: abrir o banco SQLite e montar os três repositórios na ORDEM certa.
 *
 * A ordem importa porque os dados apontam uns para os outros:
 *   1. clientes (com o limite do fiado)    -> não dependem de ninguém
 *   2. produtos                            -> não dependem de ninguém
 *   3. pedidos                             -> apontam para um cliente e para produtos
 *   4. histórico do fiado dos clientes     -> cada compra aponta para o pedido que a gerou
 */
public final class BancoSqlite {

    private BancoSqlite() {
        // Só tem o método estático abrir().
    }

    /** Cria as tabelas (se ainda não existirem) e lê tudo o que está salvo. */
    public static Repositorios abrir(ConexaoBanco banco) {
        banco.criarTabelas();
        ClienteRepositorySqlite clientes = new ClienteRepositorySqlite(banco);
        ProdutoRepositorySqlite produtos = new ProdutoRepositorySqlite(banco);
        PedidoRepositorySqlite pedidos = new PedidoRepositorySqlite(banco, clientes, produtos);
        clientes.carregarHistoricoDoFiado(pedidos);
        return new Repositorios(clientes, produtos, pedidos);
    }
}
