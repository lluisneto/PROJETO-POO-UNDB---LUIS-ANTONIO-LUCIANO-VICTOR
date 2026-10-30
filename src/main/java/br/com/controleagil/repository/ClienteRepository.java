package br.com.controleagil.repository;

import br.com.controleagil.model.Cliente;

import java.util.List;
import java.util.Optional;

/*
 * Responsabilidade: dizer O QUE dá para fazer com os clientes guardados (salvar, buscar, listar),
 * sem dizer COMO eles são guardados.
 *
 * Conceito de POO: INTERFACE (um contrato). Quem cumpre o contrato é o ClienteRepositorySqlite (no
 * sistema, guardando no banco) ou o ClienteRepositoryMemoria (nos testes). Os services só conhecem
 * esta interface, então funcionam igual com os dois.
 */
public interface ClienteRepository {

    /** Cliente sem id é inserido e ganha um id; cliente com id é atualizado. Devolve o próprio cliente. */
    Cliente salvar(Cliente cliente);

    /** Busca pelo id. Optional vazio quando não existe (em vez de devolver null). */
    Optional<Cliente> buscarPorId(long id);

    /** Todos os clientes, em ordem alfabética de nome. */
    List<Cliente> listarTodos();
}
