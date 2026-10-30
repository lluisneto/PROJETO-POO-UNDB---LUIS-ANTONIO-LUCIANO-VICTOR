package br.com.controleagil.repository;

import br.com.controleagil.model.Cliente;

import java.util.List;
import java.util.Optional;

/*
 * Responsabilidade: dizer O QUE dá para fazer com os clientes guardados (salvar, buscar, listar),
 * sem dizer COMO eles são guardados.
 *
 * Conceito de POO: INTERFACE (um contrato). Hoje quem cumpre o contrato é ClienteRepositoryMemoria
 * (lista na memória); depois será um repositório SQLite. Os services só conhecem esta interface:
 * quando o repositório SQLite cumprir o contrato inteiro (inclusive guardar a conta fiado e o
 * histórico dela), os services e as telas não precisam mudar.
 */
public interface ClienteRepository {

    /** Cliente sem id é inserido e ganha um id; cliente com id é atualizado. Devolve o próprio cliente. */
    Cliente salvar(Cliente cliente);

    /** Busca pelo id. Optional vazio quando não existe (em vez de devolver null). */
    Optional<Cliente> buscarPorId(long id);

    /** Todos os clientes, em ordem alfabética de nome. */
    List<Cliente> listarTodos();
}
