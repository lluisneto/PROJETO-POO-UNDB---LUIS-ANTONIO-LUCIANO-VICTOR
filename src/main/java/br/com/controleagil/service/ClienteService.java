package br.com.controleagil.service;

import br.com.controleagil.model.Cliente;
import br.com.controleagil.repository.ClienteRepository;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/*
 * Responsabilidade: os casos de uso de clientes: cadastrar, editar, buscar e abrir conta fiado.
 *
 * Conceito: CAMADA DE SERVIÇO. A tela (controller) pede "cadastre este cliente"; o service cria o
 * objeto do model (que valida os dados) e manda o repositório guardar. A tela nunca fala com o
 * repositório direto.
 *
 * INJEÇÃO DE DEPENDÊNCIA: o service não cria o próprio repositório; recebe pronto no construtor.
 * Como o tipo é a INTERFACE ClienteRepository, tanto faz se é memória ou SQLite.
 */
public class ClienteService {

    private final ClienteRepository repositorio;

    public ClienteService(ClienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Todos os clientes, em ordem alfabética. */
    public List<Cliente> listarTodos() {
        return repositorio.listarTodos();
    }

    /**
     * Busca por pedaço do nome (sem ligar para maiúsculas e acentos) ou do telefone (só os números).
     * Texto vazio devolve todos.
     */
    public List<Cliente> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return listarTodos();
        }
        String nomeProcurado = normalizar(texto);
        String numerosProcurados = texto.replaceAll("\\D", ""); // \D = tudo que não é dígito
        return listarTodos().stream()
                .filter(cliente -> normalizar(cliente.getNome()).contains(nomeProcurado)
                        || (!numerosProcurados.isEmpty() && cliente.getTelefone() != null
                        && cliente.getTelefone().replaceAll("\\D", "").contains(numerosProcurados)))
                .toList();
    }

    /** Cadastra um cliente novo. O nome é obrigatório; telefone e endereço são opcionais. */
    public Cliente cadastrar(String nome, String telefone, String endereco) {
        return repositorio.salvar(new Cliente(nome, telefone, endereco));
    }

    /** Atualiza os dados de um cliente já cadastrado. */
    public Cliente atualizar(Cliente cliente, String nome, String telefone, String endereco) {
        cliente.alterarNome(nome); // valida primeiro o que pode dar erro, antes de mudar o resto
        cliente.alterarTelefone(telefone);
        cliente.alterarEndereco(endereco);
        return repositorio.salvar(cliente);
    }

    /** Abre a conta fiado do cliente com o limite informado (zero ou mais). */
    public void abrirContaFiado(Cliente cliente, BigDecimal limite) {
        cliente.abrirContaFiado(limite);
        repositorio.salvar(cliente);
    }

    /** Tira acentos e deixa em minúsculas: "João" vira "joao". Assim a busca acha com ou sem acento. */
    private static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).strip();
    }
}
