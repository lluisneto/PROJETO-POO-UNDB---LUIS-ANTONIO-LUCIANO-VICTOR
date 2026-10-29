package br.com.controleagil.repository.memoria;

import br.com.controleagil.model.Produto;
import br.com.controleagil.repository.ProdutoRepository;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/*
 * Responsabilidade: guardar os produtos na memória (some ao fechar o sistema).
 * Depois, um ProdutoRepositorySqlite assume o lugar desta classe.
 *
 * Conceito de POO: IMPLEMENTAÇÃO DE INTERFACE ("implements ProdutoRepository").
 */
public class ProdutoRepositoryMemoria implements ProdutoRepository {

    private static final Comparator<Produto> POR_NOME =
            Comparator.comparing(Produto::getNome, Collator.getInstance(Locale.of("pt", "BR")));

    private final Map<Long, Produto> produtos = new LinkedHashMap<>();
    private final AtomicLong proximoId = new AtomicLong(1);

    @Override
    public Produto salvar(Produto produto) {
        if (produto.getId() == null) {
            produto.setId(proximoId.getAndIncrement());
        }
        produtos.put(produto.getId(), produto);
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
}
