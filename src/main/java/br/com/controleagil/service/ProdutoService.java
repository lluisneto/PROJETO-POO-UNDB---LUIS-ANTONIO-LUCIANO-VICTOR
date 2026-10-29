package br.com.controleagil.service;

import br.com.controleagil.model.Produto;
import br.com.controleagil.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.util.List;

/*
 * Responsabilidade: os casos de uso de produtos: cadastrar, editar e alterar preço.
 *
 * Conceito: CAMADA DE SERVIÇO com INJEÇÃO DE DEPENDÊNCIA (recebe o repositório pronto).
 */
public class ProdutoService {

    private final ProdutoRepository repositorio;

    public ProdutoService(ProdutoRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Todos os produtos, em ordem alfabética. */
    public List<Produto> listarTodos() {
        return repositorio.listarTodos();
    }

    /** Cadastra um produto novo. Nome obrigatório e preço maior que zero. */
    public Produto cadastrar(String nome, BigDecimal preco) {
        return repositorio.salvar(new Produto(nome, preco));
    }

    /**
     * Atualiza nome e preço de um produto. O preço muda por Produto.alterarPreco(): os pedidos
     * antigos continuam com o preço da época, porque cada item guardou o seu.
     */
    public Produto atualizar(Produto produto, String nome, BigDecimal novoPreco) {
        produto.alterarNome(nome);
        produto.alterarPreco(novoPreco);
        return repositorio.salvar(produto);
    }
}
