package br.com.controleagil.service;

import br.com.controleagil.exception.RegraNegocioException;
import br.com.controleagil.model.Cliente;
import br.com.controleagil.repository.memoria.ClienteRepositoryMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.com.controleagil.TesteDinheiro.reais;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Responsabilidade: testar o ClienteService: cadastro, edição, busca e conta fiado.
 */
class ClienteServiceTest {

    private ClienteService service;
    private Cliente joao;

    @BeforeEach
    void cadastrarClientes() {
        service = new ClienteService(new ClienteRepositoryMemoria());
        joao = service.cadastrar("João Pereira", "(98) 99876-5432", "Rua A, 1");
        service.cadastrar("Ana Lima", "(98) 91111-2222", null);
    }

    @Test
    @DisplayName("Lista em ordem alfabética, e os ids são gerados em sequência")
    void listaEmOrdemAlfabetica() {
        assertEquals(List.of("Ana Lima", "João Pereira"), service.listarTodos().stream().map(Cliente::getNome).toList());
        assertEquals(1L, joao.getId());
    }

    @Test
    @DisplayName("Busca pelo nome sem acento ou por pedaço do telefone")
    void buscaPorNomeOuTelefone() {
        assertEquals(List.of(joao), service.buscar("joao"));
        assertEquals(List.of(joao), service.buscar("9876"));
        assertEquals(2, service.buscar("  ").size());
    }

    @Test
    @DisplayName("Editar troca os dados; nome vazio é recusado")
    void editarCliente() {
        service.atualizar(joao, "João P. Silva", "", "Rua B, 2");
        assertEquals("João P. Silva", joao.getNome());
        assertNull(joao.getTelefone());

        assertThrows(RegraNegocioException.class, () -> service.atualizar(joao, " ", null, null));
        assertEquals("João P. Silva", joao.getNome());
    }

    @Test
    @DisplayName("Conta fiado só abre uma vez, e o limite não pode ser negativo")
    void abrirContaFiado() {
        assertThrows(RegraNegocioException.class, () -> service.abrirContaFiado(joao, reais("-1")));
        service.abrirContaFiado(joao, reais("150"));
        assertTrue(joao.temContaFiado());
        assertThrows(RegraNegocioException.class, () -> service.abrirContaFiado(joao, reais("200")));
    }
}
