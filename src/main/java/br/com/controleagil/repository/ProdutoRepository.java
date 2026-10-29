package br.com.controleagil.repository;

import br.com.controleagil.model.Produto;

import java.util.List;
import java.util.Optional;

/*
 * Responsabilidade: contrato para guardar e buscar produtos.
 *
 * Conceito de POO: INTERFACE. Mesma ideia do ClienteRepository: hoje a implementação é em
 * memória; amanhã, SQLite. Quem usa (o ProdutoService) não percebe a troca.
 */
public interface ProdutoRepository {

    /** Produto sem id é inserido e ganha um id; produto com id é atualizado. */
    Produto salvar(Produto produto);

    /** Busca pelo id. Optional vazio quando não existe. */
    Optional<Produto> buscarPorId(long id);

    /** Todos os produtos, em ordem alfabética de nome. */
    List<Produto> listarTodos();
}
