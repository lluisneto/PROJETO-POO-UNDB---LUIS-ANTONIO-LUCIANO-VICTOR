package br.com.controleagil.repository;

import br.com.controleagil.repository.memoria.ClienteRepositoryMemoria;
import br.com.controleagil.repository.memoria.PedidoRepositoryMemoria;
import br.com.controleagil.repository.memoria.ProdutoRepositoryMemoria;

/*
 * Responsabilidade: juntar os três repositórios do sistema num objeto só, para quem monta o
 * sistema (o AppContexto) receber "os repositórios" sem saber se são do SQLite ou da memória.
 *
 * Conceito: RECORD (Java 16+), uma classe só de dados: o Java cria o construtor e os métodos
 * clientes(), produtos() e pedidos() sozinho.
 */
public record Repositorios(ClienteRepository clientes, ProdutoRepository produtos, PedidoRepository pedidos) {

    /** Repositórios em memória: nada é salvo em arquivo (usado nos testes). */
    public static Repositorios emMemoria() {
        return new Repositorios(new ClienteRepositoryMemoria(), new ProdutoRepositoryMemoria(),
                new PedidoRepositoryMemoria());
    }
}
