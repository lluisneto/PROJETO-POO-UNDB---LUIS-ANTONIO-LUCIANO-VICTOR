package br.com.controleagil.repository;

import br.com.controleagil.model.Produto;

import java.util.List;
import java.util.Optional;

/*
 * Responsabilidade: contrato para guardar e buscar produtos.
 *
 * Conceito de POO: INTERFACE. Mesma ideia do ClienteRepository: no sistema a implementação é a do
 * SQLite; nos testes, a da memória. Quem usa (o ProdutoService) não percebe a diferença.
 */
public interface ProdutoRepository {

    /** Produto sem id é inserido e ganha um id; produto com id é atualizado. */
    Produto salvar(Produto produto);

    /** Busca pelo id. Optional vazio quando não existe. */
    Optional<Produto> buscarPorId(long id);

    /** Todos os produtos, em ordem alfabética de nome. */
    List<Produto> listarTodos();
}
