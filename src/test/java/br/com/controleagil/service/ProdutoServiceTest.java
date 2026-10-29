package br.com.controleagil.service;

import br.com.controleagil.exception.RegraNegocioException;
import br.com.controleagil.model.Produto;
import br.com.controleagil.repository.memoria.ProdutoRepositoryMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static br.com.controleagil.TesteDinheiro.assertValor;
import static br.com.controleagil.TesteDinheiro.reais;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 * Responsabilidade: testar o ProdutoService: cadastro e edição sem deixar o produto "pela metade".
 */
class ProdutoServiceTest {

    private ProdutoService service;
    private Produto coxinha;

    @BeforeEach
    void cadastrar() {
        service = new ProdutoService(new ProdutoRepositoryMemoria());
        coxinha = service.cadastrar("Coxinha", reais("5.00"));
    }

    @Test
    @DisplayName("Editar troca nome e preço")
    void editarProduto() {
        service.atualizar(coxinha, "Coxinha grande", reais("6.50"));

        assertEquals("Coxinha grande", coxinha.getNome());
        assertValor("6.50", coxinha.getPreco());
    }

    @Test
    @DisplayName("Preço inválido não muda nada, nem o nome")
    void precoInvalidoNaoMudaNada() {
        assertThrows(RegraNegocioException.class, () -> service.atualizar(coxinha, "Coxinha grande", reais("0")));

        assertEquals("Coxinha", coxinha.getNome());
        assertValor("5.00", coxinha.getPreco());
    }

    @Test
    @DisplayName("Cadastro sem nome é recusado")
    void cadastroSemNome() {
        assertThrows(RegraNegocioException.class, () -> service.cadastrar(" ", reais("5.00")));
        assertEquals(1, service.listarTodos().size());
    }
}
