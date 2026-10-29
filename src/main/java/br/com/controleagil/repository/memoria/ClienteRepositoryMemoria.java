package br.com.controleagil.repository.memoria;

import br.com.controleagil.model.Cliente;
import br.com.controleagil.repository.ClienteRepository;

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
 * Responsabilidade: guardar os clientes num Map, na memória do computador.
 * Tudo some quando o sistema fecha: é só para o esqueleto da interface funcionar.
 * Depois, um ClienteRepositorySqlite assume o lugar desta classe.
 *
 * Conceito de POO: IMPLEMENTAÇÃO DE INTERFACE ("implements ClienteRepository"). Esta classe
 * promete cumprir o contrato da interface; o @Override confirma que cada método é dela.
 */
public class ClienteRepositoryMemoria implements ClienteRepository {

    /** Collator ordena como no dicionário em português: "Ávila" fica junto de "Ana", não no fim. */
    private static final Comparator<Cliente> POR_NOME =
            Comparator.comparing(Cliente::getNome, Collator.getInstance(Locale.of("pt", "BR")));

    private final Map<Long, Cliente> clientes = new LinkedHashMap<>();

    /** Gera os ids 1, 2, 3... (AtomicLong é um contador que não repete número). */
    private final AtomicLong proximoId = new AtomicLong(1);

    @Override
    public Cliente salvar(Cliente cliente) {
        if (cliente.getId() == null) {
            cliente.setId(proximoId.getAndIncrement());
        }
        // Na memória, o objeto guardado é o mesmo que a tela usa; "salvar" só garante que está no Map.
        clientes.put(cliente.getId(), cliente);
        return cliente;
    }

    @Override
    public Optional<Cliente> buscarPorId(long id) {
        return Optional.ofNullable(clientes.get(id));
    }

    @Override
    public List<Cliente> listarTodos() {
        List<Cliente> lista = new ArrayList<>(clientes.values());
        lista.sort(POR_NOME);
        return List.copyOf(lista); // cópia somente leitura: quem recebe não mexe no Map daqui
    }
}
