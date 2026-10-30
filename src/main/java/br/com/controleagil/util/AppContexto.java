package br.com.controleagil.util;

import br.com.controleagil.controller.AgendaController;
import br.com.controleagil.controller.ClientesController;
import br.com.controleagil.controller.FiadoController;
import br.com.controleagil.controller.InicioController;
import br.com.controleagil.controller.Navegacao;
import br.com.controleagil.controller.NovoPedidoController;
import br.com.controleagil.controller.PrincipalController;
import br.com.controleagil.controller.ProdutosController;
import br.com.controleagil.repository.ClienteRepository;
import br.com.controleagil.repository.Repositorios;
import br.com.controleagil.service.ClienteService;
import br.com.controleagil.service.DadosExemplo;
import br.com.controleagil.service.FiadoService;
import br.com.controleagil.service.PedidoService;
import br.com.controleagil.service.ProdutoService;

import java.time.Clock;

/*
 * Responsabilidade: MONTAR o sistema. Cria os repositórios e os services UMA vez só e entrega
 * para cada controller o que ele precisa.
 *
 * Conceito: INJEÇÃO DE DEPENDÊNCIA (feita à mão, sem biblioteca).
 * Em vez de cada controller fazer "new PedidoService(new PedidoRepositoryMemoria())" por conta
 * própria, ele RECEBE os services prontos no construtor. Vantagens:
 *  1. Todas as telas usam os MESMOS objetos: o pedido salvo no "Novo pedido" aparece na Agenda.
 *     (Se cada tela criasse o seu repositório em memória, cada uma teria uma lista diferente!)
 *  2. O AppContexto não sabe se os dados ficam no SQLite ou na memória: ele recebe os
 *     "Repositorios" prontos. O App usa o SQLite; os testes podem usar a memória.
 *  3. Nos testes, dá para montar um service com um repositório e um relógio de mentira.
 *
 * Como o controller chega até aqui? O FXMLLoader normalmente cria o controller sozinho, com o
 * construtor vazio. Com setControllerFactory(...), ele pede o controller para criarController(),
 * que chama o construtor certo passando os services.
 */
public class AppContexto {

    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final PedidoService pedidoService;
    private final FiadoService fiadoService;
    private final String mensagemInicial;

    /**
     * @param repositorios      onde os dados ficam (SQLite no sistema de verdade)
     * @param carregarExemplos  true para encher o sistema com os dados fictícios de DadosExemplo
     * @param mensagemInicial   texto mostrado na barra de status quando o sistema abre
     */
    public AppContexto(Clock relogio, Repositorios repositorios, boolean carregarExemplos, String mensagemInicial) {
        ClienteRepository clientes = repositorios.clientes();
        clienteService = new ClienteService(clientes);
        produtoService = new ProdutoService(repositorios.produtos());
        pedidoService = new PedidoService(repositorios.pedidos(), relogio);
        fiadoService = new FiadoService(clientes);
        this.mensagemInicial = mensagemInicial;

        if (carregarExemplos) {
            DadosExemplo.carregar(clienteService, produtoService, pedidoService, fiadoService, relogio);
        }
    }

    /** Texto para a barra de status na abertura (ex.: de onde os dados foram lidos). */
    public String getMensagemInicial() {
        return mensagemInicial;
    }

    /**
     * Cria o controller pedido pelo FXMLLoader, já com as dependências.
     *
     * @param tipo       classe escrita no fx:controller do arquivo .fxml
     * @param navegacao  quem abre telas e mostra mensagens (a janela principal); null para a própria principal
     */
    public Object criarController(Class<?> tipo, Navegacao navegacao) {
        if (tipo == PrincipalController.class) {
            return new PrincipalController(this);
        }
        if (tipo == InicioController.class) {
            return new InicioController(pedidoService, fiadoService, navegacao);
        }
        if (tipo == AgendaController.class) {
            return new AgendaController(pedidoService, navegacao);
        }
        if (tipo == NovoPedidoController.class) {
            return new NovoPedidoController(clienteService, produtoService, pedidoService, navegacao);
        }
        if (tipo == ClientesController.class) {
            return new ClientesController(clienteService, navegacao);
        }
        if (tipo == ProdutosController.class) {
            return new ProdutosController(produtoService, navegacao);
        }
        if (tipo == FiadoController.class) {
            return new FiadoController(fiadoService, navegacao);
        }
        throw new IllegalArgumentException("O AppContexto não sabe criar o controller " + tipo.getName());
    }
}
